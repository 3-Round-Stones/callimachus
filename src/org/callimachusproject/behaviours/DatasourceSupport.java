/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.http.HttpEntity;
import org.callimachusproject.io.DescribeResult;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.server.helpers.RequestActivityFactory;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Sparql;
import org.openrdf.http.object.client.CloseableEntity;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.http.object.fluid.FluidBuilder;
import org.openrdf.http.object.fluid.FluidException;
import org.openrdf.http.object.fluid.FluidFactory;
import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserRegistry;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatasourceSupport implements CalliObject {
	private static final String GET_GRAPH = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH $graph { ?s ?p ?o } }";
	private static final String GET_DEFAULT = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
	private static final String PREFIX = "PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>\n";
	private static final Pattern HAS_PREFIX = Pattern.compile("^[^#]*\\bPREFIX\\b",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	private final Logger logger = LoggerFactory.getLogger(DatasourceSupport.class);

	@Sparql(PREFIX + "ASK { $this sd:supportedLanguage sd:SPARQL11Query }")
	public abstract boolean isQuerySupported();

	@Sparql(PREFIX + "ASK { $this sd:supportedLanguage sd:SPARQL11Update }")
	public abstract boolean isUpdateSupported();

	public GraphQueryResult describeResource(URI uri)
			throws OpenRDFException, IOException, DatatypeConfigurationException {
		if (uri == null)
			throw new BadRequest("Missing uri");
		if (!isQuerySupported())
			throw new BadRequest("SPARQL Query is not supported on this service");
		return new DescribeResult(uri, openConnection(), true);
	}

	public GraphQueryResult constructGraph(URI graph)
			throws OpenRDFException, IOException, DatatypeConfigurationException {
		boolean success = false;
		final RepositoryConnection con = openConnection();
		try {
			GraphQuery rq;
			if (graph == null) {
				rq = con.prepareGraphQuery(SPARQL, GET_DEFAULT);
			} else {
				rq = con.prepareGraphQuery(SPARQL, GET_GRAPH);
				rq.setBinding("graph", graph);
			}
			GraphQueryResult rdf = rq.evaluate();
			rdf = new GraphQueryResultImpl(rdf.getNamespaces(), rdf) {
				protected void handleClose() throws QueryEvaluationException {
					super.handleClose();
					try {
						con.close();
					} catch (RepositoryException e) {
						throw new QueryEvaluationException(e);
					}
				}
			};
			success = true;
			return rdf;
		} finally {
			if (!success) {
				con.close();
			}
		}
	}

	public void dropGraph(URI graph)
			throws OpenRDFException, IOException, DatatypeConfigurationException {
		final RepositoryConnection con = openConnection();
		try {
			if (graph == null) {
				con.prepareUpdate(SPARQL, "DROP DEFAULT").execute();
			} else {
				if (graph.stringValue().contains(">"))
					throw new BadRequest("Invalid graph URI: " + graph);
				String ru = "DROP GRAPH <" + graph.stringValue() + ">";
				con.prepareUpdate(SPARQL, ru, this.toString()).execute();
			}
		} finally {
			con.close();
		}
	}

	public boolean loadGraph(InputStream rdf, String type, URI graph)
			throws OpenRDFException, IOException, DatatypeConfigurationException {
		final RepositoryConnection con = openConnection();
		try {
			con.begin();
			boolean created = !con.hasStatement(null, null, null, true, graph);
			con.add(rdf, this.toString(), RDFFormat.forMIMEType(type), graph);
			con.commit();
			mergeNamespaces(con.getNamespaces());
			return created;
		} finally {
			con.rollback();
			con.close();
		}
	}

	public boolean clearAndLoadGraph(InputStream rdf, String type, URI graph)
			throws OpenRDFException, IOException, DatatypeConfigurationException {
		final RepositoryConnection con = openConnection();
		try {
			con.begin();
			boolean created = !con.hasStatement(null, null, null, true, graph);
			con.clear(graph);
			con.add(rdf, this.toString(), RDFFormat.forMIMEType(type), graph);
			con.commit();
			mergeNamespaces(con.getNamespaces());
			return created;
		} finally {
			con.rollback();
			con.close();
		}
	}

	public void patchGraph(String sparql, URI graph)
			throws OpenRDFException, IOException, DatatypeConfigurationException {
		final RepositoryConnection con = openConnection();
		try {
			con.begin();
			Update ru = con.prepareUpdate(SPARQL, sparql, this.toString());
			DatasetImpl ds = new DatasetImpl();
			ds.setDefaultInsertGraph(graph);
			ds.addDefaultGraph(graph);
			ds.addDefaultRemoveGraph(graph);
			ru.setDataset(ds);
			ru.execute();
			con.commit();
		} finally {
			con.rollback();
			con.close();
		}
	}

	public HttpEntity evaluateSparql(String qry) throws OpenRDFException,
			IOException, FluidException, DatatypeConfigurationException {
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

	public void executeSparql(String ru) throws OpenRDFException, IOException, DatatypeConfigurationException {
		if (!isUpdateSupported())
			throw new BadRequest("SPARQL Update is not supported on this service");
		String update = addPrefix(ru);
		RepositoryConnection con = openConnection();
		try {
			String base = this.getResource().stringValue();
			con.prepareUpdate(QueryLanguage.SPARQL, update, base).execute();
			logger.info(update);
			mergeNamespaces(con.getNamespaces());
		} finally {
			con.close();
		}
	}

	public void purgeDatasource() throws OpenRDFException, IOException {
		URI uri = (URI) this.getResource();
		String id = getCalliRepository().getDatasourceRepositoryId(uri);
		RepositoryManager manager = getCalliRepository().getRepositoryManager();
		manager.removeRepository(id);
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

	private RepositoryConnection openConnection() throws OpenRDFException,
			IOException, DatatypeConfigurationException {
		URI uri = (URI) this.getResource();
		ObjectConnection con1 = this.getObjectConnection();
		URI bundle = con1.getVersionBundle();
		AuditingRepositoryConnection audit1 = findAuditing(con1);
		String id = getCalliRepository().getDatasourceRepositoryId(uri);
		RepositoryManager manager = getCalliRepository().getRepositoryManager();
		if (manager == null)
			throw new IllegalArgumentException(
					"Datasources are not configured correctly");
		if (!manager.hasRepositoryConfig(id)) {
			manager.addRepositoryConfig(new RepositoryConfig(id, uri
					.stringValue(), getDefaultConfig()));
		
		}
		Repository repo2 = manager.getRepository(id);
		RepositoryConnection con2 = repo2.getConnection();
		AuditingRepositoryConnection audit2 = findAuditing(con2);
		if (bundle != null && audit1 != null && audit2 != null) {
			ActivityFactory af1 = audit1.getActivityFactory();
			ActivityFactory af2 = audit2.getActivityFactory();
			if (af1 instanceof RequestActivityFactory && af2 != null) {
				RequestActivityFactory raf = (RequestActivityFactory) af1;
				RequestActivityFactory af = new RequestActivityFactory(raf, af2);
				audit2.setActivityFactory(af);
			}
		}
		if (con2 instanceof ContextAwareConnection) {
			ContextAwareConnection con3 = (ContextAwareConnection) con2;
			con3.setInsertContext(con1.getVersionBundle());
			return con3;
		} else {
			ContextAwareConnection con3 = new ContextAwareConnection(con2);
			con3.setInsertContext(con1.getVersionBundle());
			return con3;
		}
	}

	private SailRepositoryConfig getDefaultConfig() {
		String indices = "spoc,pocs,oscp,cspo";
		return new SailRepositoryConfig(new NativeStoreConfig(indices));
	}

	private void mergeNamespaces(RepositoryResult<Namespace> namespaces) throws RepositoryException {
		ObjectConnection con = this.getObjectConnection();
		Map<String, String> map = new LinkedHashMap<String, String>();
		try {
			while (namespaces.hasNext()) {
				Namespace ns = namespaces.next();
				if (con.getNamespace(ns.getPrefix()) == null) {
					map.put(ns.getPrefix(), ns.getName());
				}
			}
		} finally {
			namespaces.close();
		}
		for (Map.Entry<String, String> e : map.entrySet()) {
			con.setNamespace(e.getKey(), e.getValue());
		}
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
}
