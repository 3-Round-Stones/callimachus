package org.callimachusproject.behaviours;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.callimachusproject.client.CloseableEntity;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.repository.DatasourceManager;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserRegistry;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatasourceSupport implements CalliObject {
	private static final String PREFIX = "PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>\n";
	private static final Pattern HAS_PREFIX = Pattern.compile("^[^#]*\\bPREFIX\\b",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	private final Logger logger = LoggerFactory.getLogger(DatasourceSupport.class);

	@Sparql(PREFIX + "ASK { $this sd:supportedLanguage sd:SPARQL11Query }")
	public abstract boolean isQuerySupported();

	@Sparql(PREFIX + "ASK { $this sd:supportedLanguage sd:SPARQL11Update }")
	public abstract boolean isUpdateSupported();

	public GraphQueryResult describeResource(final URI uri)
			throws OpenRDFException, IOException {
		if (uri == null)
			throw new BadRequest("Missing uri");
		if (!isQuerySupported())
			throw new BadRequest("SPARQL Query is not supported on this service");
		return new DescribeResult(uri, openConnection());
	}

	public HttpEntity evaluateSparql(String qry) throws OpenRDFException,
			IOException, FluidException {
		if (qry == null || qry.length() == 0)
			throw new BadRequest("Missing query");
		if (!isQuerySupported())
			throw new BadRequest("SPARQL Query is not supported on this service");
		String query = addPrefix(qry);
		final RepositoryConnection con = openConnection();
		try {
			String mime;
			Object rs;
			Class<?> type;
			String base = this.getResource().stringValue();
			QueryParserRegistry reg = QueryParserRegistry.getInstance();
			QueryParser parser = reg.get(QueryLanguage.SPARQL).getParser();
			ParsedQuery parsed = parser.parseQuery(query, base);
			if (parsed instanceof ParsedBooleanQuery) {
				mime = "application/sparql-results+xml";
				rs = con.prepareBooleanQuery(QueryLanguage.SPARQL, query, base)
						.evaluate();
				type = java.lang.Boolean.TYPE;
			} else if (parsed instanceof ParsedGraphQuery) {
				mime = "application/rdf+xml";
				rs = con.prepareGraphQuery(QueryLanguage.SPARQL, query, base)
						.evaluate();
				type = rs.getClass();
			} else if (parsed instanceof ParsedTupleQuery) {
				mime = "application/sparql-results+xml";
				rs = con.prepareTupleQuery(QueryLanguage.SPARQL, query, base)
						.evaluate();
				type = rs.getClass();
			} else {
				throw new InternalServerError("Unknown query type: "
						+ parsed.getClass());
			}
			FluidBuilder fb = FluidFactory.getInstance().builder();
			return new CloseableEntity(fb.consume(rs, base, type, mime)
					.asHttpEntity(), new Closeable() {
				public void close() throws IOException {
					try {
						con.close();
					} catch (RepositoryException e) {
						logger.error(e.toString(), e);
					}
				}
			});
		} catch (MalformedQueryException e) {
			con.close();
			throw new BadRequest(e.toString());
		} catch (IllegalArgumentException e) {
			con.close();
			throw new BadRequest("Missing accept header: " + e.getMessage());
		} catch (RuntimeException e) {
			con.close();
			throw e;
		} catch (Error e) {
			con.close();
			throw e;
		}
	}

	public void executeSparql(String ru) throws OpenRDFException, IOException {
		if (!isUpdateSupported())
			throw new BadRequest("SPARQL Update is not supported on this service");
		String update = addPrefix(ru);
		RepositoryConnection con = openConnection();
		try {
			String base = this.getResource().stringValue();
			con.prepareUpdate(QueryLanguage.SPARQL, update, base).execute();
			logger.info(update);
		} finally {
			con.close();
		}
	}

	private String addPrefix(String inputString) throws RepositoryException {
		if (HAS_PREFIX.matcher(inputString).find())
			return inputString;
		StringBuilder sb = new StringBuilder();
		ObjectConnection con = this.getObjectConnection();
		RepositoryResult<Namespace> namespaces = con.getNamespaces();
		try {
			while (namespaces.hasNext()) {
				Namespace namespace = namespaces.next();
				String prefix = namespace.getPrefix();
				String uri = namespace.getName();
				if (inputString.indexOf(prefix) >= 0) {
					sb.append("PREFIX ").append(prefix);
					sb.append(":<").append(uri).append(">\n");
				}
			}
			return sb.append(inputString).toString();
		} finally {
			namespaces.close();
		}
	}

	private RepositoryConnection openConnection() throws OpenRDFException, IOException {
		URI uri = (URI) this.getResource();
		ObjectConnection con1 = this.getObjectConnection();
		URI bundle = con1.getVersionBundle();
		AuditingRepositoryConnection audit1 = findAuditing(con1);
		DatasourceManager manager = getCalliRepository().getDatasourceManager();
		if (manager == null)
			throw new IllegalArgumentException(
					"Datasources are not configured correctly");
		if (!manager.isDatasourcePresent(uri)) {
			manager.setDatasourceConfig(uri, getDefaultConfig());
		}
		CalliRepository repo2 = manager.getDatasource(uri);
		ObjectConnection con2 = repo2.getDelegate().getConnection();
		AuditingRepositoryConnection audit2 = findAuditing(con2);
		if (audit1 != null && audit2 != null && bundle != null) {
			audit2.setActivityFactory(audit1.getActivityFactory());
		}
		con2.setVersionBundle(con1.getVersionBundle());
		con2.setInsertContext(con1.getInsertContext());
		return con2;
	}

	private SailRepositoryConfig getDefaultConfig() {
		String indices = "spoc,pocs,oscp,cspo";
		return new SailRepositoryConfig(new NativeStoreConfig(indices));
	}

	private AuditingRepositoryConnection findAuditing(RepositoryConnection con)
			throws RepositoryException {
		if (con instanceof AuditingRepositoryConnection)
			return (AuditingRepositoryConnection) con;
		if (con instanceof RepositoryConnectionWrapper)
			return findAuditing(((RepositoryConnectionWrapper) con)
					.getDelegate());
		return null;
	}

	private static class DescribeResult implements GraphQueryResult {
		private final Set<Resource> seen = new HashSet<Resource>();
		private final LinkedList<Resource> queue = new LinkedList<Resource>();
		private final String base;
		private final boolean baseIsHash;
		private final RepositoryConnection con;
		private RepositoryResult<Statement> stmts;
		private Statement last;

		private DescribeResult(URI resource, RepositoryConnection toBeClosed)
				throws OpenRDFException {
			this.con = toBeClosed;
			seen.add(resource);
			queue.push(resource);
			base = resource.stringValue();
			baseIsHash = base.charAt(base.length() - 1) == '#';
			stmts = con.getStatements(null, RDFS.ISDEFINEDBY, resource, false);
			try {
				while (stmts.hasNext()) {
					pushIfHash(stmts.next().getSubject());
				}
			} finally {
				stmts.close();
			}
		}

		public void close() throws QueryEvaluationException {
			try {
				try {
					stmts.close();
				} finally {
					con.close();
				}
			} catch (RepositoryException e) {
				throw new QueryEvaluationException(e);
			}
		}

		public Map<String, String> getNamespaces()
				throws QueryEvaluationException {
			try {
				RepositoryResult<Namespace> namespaces = con.getNamespaces();
				Map<String, String> map = new LinkedHashMap<String, String>();
				while (namespaces.hasNext()) {
					Namespace ns = namespaces.next();
					map.put(ns.getPrefix(), ns.getName());
				}
				return map;
			} catch (RepositoryException e) {
				throw new QueryEvaluationException(e);
			}
		}

		public boolean hasNext() throws QueryEvaluationException {
			try {
				while (!stmts.hasNext() && queue.size() > 0) {
					stmts.close();
					stmts = con.getStatements(queue.poll(), null, null, false);
				}
				return stmts.hasNext();
			} catch (RepositoryException e) {
				throw new QueryEvaluationException(e);
			}
		}

		public Statement next() throws QueryEvaluationException {
			try {
				while (!stmts.hasNext() && queue.size() > 0) {
					stmts.close();
					stmts = con.getStatements(queue.poll(), null, null, false);
				}
				Statement st = stmts.next();
				while (last != null && stmts.hasNext()
						&& st.getSubject() == last.getSubject()
						&& st.getPredicate() == last.getPredicate()
						&& st.getObject() == last.getObject()) {
					st = stmts.next();
				}
				pushIfHash(st.getObject());
				last = st;
				return st;
			} catch (RepositoryException e) {
				throw new QueryEvaluationException(e);
			}
		}

		public void remove() throws QueryEvaluationException {
			try {
				stmts.remove();
			} catch (RepositoryException e) {
				throw new QueryEvaluationException(e);
			}
		}

		private void pushIfHash(Value object) {
			String uri = object.stringValue();
			if (object instanceof URI) {
				if (uri.length() > base.length() && uri.indexOf(base) == 0) {
					char chr = uri.charAt(base.length());
					if (baseIsHash || chr == '#' && !seen.contains(object)) {
						seen.add((URI) object);
						queue.push((URI) object);
					}
				}
			} else if (object instanceof Resource) {
				if (!seen.contains(object)) {
					seen.add((Resource) object);
					queue.push((Resource) object);
				}
			}
		}
	}
}
