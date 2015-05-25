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

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.io.SparqlInsertDataParser;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.server.helpers.RequestActivityFactory;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.HeaderParam;
import org.openrdf.annotations.Method;
import org.openrdf.annotations.Param;
import org.openrdf.annotations.Path;
import org.openrdf.annotations.Sparql;
import org.openrdf.annotations.Type;
import org.openrdf.http.object.client.CloseableEntity;
import org.openrdf.http.object.client.HttpUriResponse;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.fluid.FluidBuilder;
import org.openrdf.http.object.fluid.FluidException;
import org.openrdf.http.object.fluid.FluidFactory;
import org.openrdf.http.object.fluid.FluidType;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.Update;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserRegistry;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultWriterRegistry;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriterRegistry;
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
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatasourceSupport extends GraphStoreSupport implements CalliObject {
	private static final int MAX_RU_SIZE = 10240; // 10KiB
	private static final String PREFIX = "PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>\n";
	static final String TOO_MANY_RESULTS = "http://callimachusproject.org/rdf/2009/framework#hasResultLimit";

	final Logger logger = LoggerFactory.getLogger(DatasourceSupport.class);
	private final BooleanQueryResultWriterRegistry bool = BooleanQueryResultWriterRegistry.getInstance();
	private final TupleQueryResultWriterRegistry tuple = TupleQueryResultWriterRegistry.getInstance();

	@Sparql(PREFIX + "ASK { $this sd:supportedLanguage sd:SPARQL11Query }")
	public abstract boolean isQuerySupported();

	@Sparql(PREFIX + "ASK { $this sd:supportedLanguage sd:SPARQL11Update }")
	public abstract boolean isUpdateSupported();

	@Override
	@Method("POST")
	@Path("\\?.*|$")
	@requires(READER)
	public HttpUriResponse postQuery(
			@Param("default-graph-uri") String[] defaultGraphs,
			@Param("named-graph-uri") String[] namedGraphs,
			@HeaderParam("Accept") String accept,
			@HeaderParam("Content-Location") String loc,
			@Type("application/sparql-query") byte[] rq) throws IOException,
			OpenRDFException {
		if (rq == null || rq.length == 0)
			throw new BadRequest("Missing query");
		if (!isQuerySupported())
			throw new BadRequest("SPARQL Query is not supported on this service");
		String query = new String(rq, Consts.UTF_8);
		boolean close = true;
		final RepositoryConnection con = openConnection();
		try {
			String mime;
			Object rs;
			Class<?> type;
			final String base = this.getResource().stringValue();
			QueryParserRegistry reg = QueryParserRegistry.getInstance();
			QueryParser parser = reg.get(QueryLanguage.SPARQL).getParser();
			ParsedQuery parsed = parser.parseQuery(query, loc == null ? base : loc);
			if (parsed instanceof ParsedBooleanQuery) {
				mime = getAcceptableBooleanFormat(accept).getDefaultMIMEType();
				BooleanQuery pq = con.prepareBooleanQuery(QueryLanguage.SPARQL, query, base);
				if (defaultGraphs != null || namedGraphs != null) {
					pq.setDataset(createDataset(defaultGraphs, namedGraphs));
				}
				rs = pq.evaluate();
				type = java.lang.Boolean.TYPE;
			} else if (parsed instanceof ParsedGraphQuery) {
				mime = getAcceptableRDFFormat(accept).getDefaultMIMEType();
				GraphQuery pq = con.prepareGraphQuery(QueryLanguage.SPARQL, query, base);
				if (defaultGraphs != null || namedGraphs != null) {
					pq.setDataset(createDataset(defaultGraphs, namedGraphs));
				}
				rs = pq.evaluate();
				type = rs.getClass();
			} else if (parsed instanceof ParsedTupleQuery) {
				mime = getAcceptableTupleFormat(accept).getDefaultMIMEType();
				TupleQuery pq = con.prepareTupleQuery(QueryLanguage.SPARQL, query, base);
				if (defaultGraphs != null || namedGraphs != null) {
					pq.setDataset(createDataset(defaultGraphs, namedGraphs));
				}
				rs = pq.evaluate();
				type = rs.getClass();
			} else {
				throw new InternalServerError("Unknown query type: "
						+ parsed.getClass());
			}
			FluidBuilder fb = FluidFactory.getInstance().builder();
			BasicHttpResponse res = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
			res.setEntity(new CloseableEntity(fb.consume(rs, base, type, mime)
					.asHttpEntity(), new Closeable() {
				public void close() throws IOException {
					try {
						con.close();
					} catch (RepositoryException e) {
						logger.error(e.toString(), e);
					}
				}
			}));
			close = false;
			return new HttpUriResponse(this.toString(), res);
		} catch (FluidException e) {
			throw new InternalServerError(e);
		} catch (MalformedQueryException e) {
			throw new BadRequest(e.toString());
		} catch (IllegalArgumentException e) {
			throw new BadRequest("Missing accept header: " + e.getMessage());
		} finally {
			if (close) {
				con.close();
			}
		}
	}

	@Override
	@Method("POST")
	@Path("\\?.*|$")
	@requires(EDITOR)
	public HttpResponse postUpdate(
			@Param("using-graph-uri") final String[] defaultGraphs,
			@Param("using-named-graph-uri") final String[] namedGraphs,
			@HeaderParam("Content-Location") String loc,
			@Type("application/sparql-update") InputStream ru)
			throws IOException, OpenRDFException {
		if (!isUpdateSupported())
			throw new BadRequest("SPARQL Update is not supported on this service");
		BufferedInputStream bin = new BufferedInputStream(ru, MAX_RU_SIZE);
		bin.mark(MAX_RU_SIZE);
		int skipped = (int) bin.skip(MAX_RU_SIZE);
		bin.reset();
		String base = loc == null ? this.getResource().stringValue() : loc;
		final RepositoryConnection con = openConnection();
		try {
			if (MAX_RU_SIZE > skipped) {
				byte[] buf = new byte[skipped];
				try {
					bin.read(buf);
				} finally {
					bin.close();
				}
				String update = new String(buf, Consts.UTF_8);
				Update pu = con.prepareUpdate(SPARQL, update, base);
				if (defaultGraphs != null || namedGraphs != null) {
					pu.setDataset(createDataset(defaultGraphs, namedGraphs));
				}
				pu.execute();
				logger.info(update);
				mergeNamespaces(con.getNamespaces());
			} else if (defaultGraphs == null || defaultGraphs.length == 1 && namedGraphs == null) {
				// large INSERT DATA
				try {
					RDFParser parser = new SparqlDropInsertDataParser(
							defaultGraphs, namedGraphs, con);
					RDFInserter inserter = new RDFInserter(con);
					if (defaultGraphs != null && defaultGraphs.length == 1) {
						inserter.enforceContext(vf.createURI(defaultGraphs[0]));
						logger.info("INSERT DATA { GRAPH <{}> }",
								defaultGraphs[0]);
					} else {
						logger.info("INSERT DATA", defaultGraphs[0]);
					}
					parser.setRDFHandler(inserter);
					parser.parse(bin, base);
				} finally {
					bin.close();
				}
			} else {
				bin.close();
				throw new ResponseException("Request Entity Too Large") {
					public int getStatusCode() {
						return 413;
					}
				};
			}
			return new BasicHttpResponse(HttpVersion.HTTP_1_1, 204, "No Content");
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

	private BooleanQueryResultFormat getAcceptableBooleanFormat(String accept) {
		String type = new FluidType(Model.class, "text/boolean",
				"application/json", "application/sparql-results+xml").as(
				accept.split("\\s*,\\s*")).preferred();
		return bool.getFileFormatForMIMEType(type,
				BooleanQueryResultFormat.TEXT);
	}

	private TupleQueryResultFormat getAcceptableTupleFormat(String accept) {
		String type = new FluidType(Model.class, "text/csv",
				"application/json", "application/sparql-results+xml",
				"text/tab-separated-values").as(accept.split("\\s*,\\s*"))
				.preferred();
		return tuple.getFileFormatForMIMEType(type, TupleQueryResultFormat.CSV);
	}

	private Dataset createDataset(String[] defaultGraphURIs,
			String[] namedGraphURIs) {
		ValueFactory vf = this.getObjectConnection().getValueFactory();
		DatasetImpl dataset = new DatasetImpl();
		if (defaultGraphURIs != null) {
			for (String defaultGraphURI : defaultGraphURIs) {
				try {
					URI uri = vf.createURI(defaultGraphURI);
					dataset.addDefaultGraph(uri);
					dataset.addDefaultRemoveGraph(uri);
					if (defaultGraphURIs.length == 1) {
						dataset.setDefaultInsertGraph(uri);
					}
				} catch (IllegalArgumentException e) {
					throw new BadRequest("Illegal URI for default graph: "
							+ defaultGraphURI);
				}
			}
		}
		if (namedGraphURIs != null) {
			for (String namedGraphURI : namedGraphURIs) {
				try {
					URI uri = vf.createURI(namedGraphURI);
					dataset.addNamedGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new BadRequest("Illegal URI for named graph: "
							+ namedGraphURI);
				}
			}
		}
		return dataset;
	}

	private RepositoryConnection openConnection() throws OpenRDFException,
			IOException {
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

	static class SparqlDropInsertDataParser extends SparqlInsertDataParser {
		private final String[] namedGraphs;
		private final RepositoryConnection con;
		private final String[] defaultGraphs;
		private final ValueFactory vf;
	
		SparqlDropInsertDataParser(String[] defaultGraphs,
				String[] namedGraphs, RepositoryConnection con) {
			super(con.getValueFactory());
			this.namedGraphs = namedGraphs;
			this.con = con;
			this.defaultGraphs = defaultGraphs;
			this.vf = con.getValueFactory();
		}
	
		@Override
		protected void reportDropGraph(URI graph)
				throws RDFHandlerException {
			try {
				con.clear(graph);
			} catch (RepositoryException e) {
				throw new RDFHandlerException(e);
			}
		}
	
		@Override
		protected void reportDropDefault()
				throws RDFHandlerException {
			try {
				if (defaultGraphs != null && defaultGraphs.length > 0) {
					Resource[] contexts = new Resource[defaultGraphs.length];
					for (int i=0;i<defaultGraphs.length;i++) {
						contexts[i] = vf.createURI(defaultGraphs[i]);
					}
					con.clear(contexts);
				} else {
					con.clear((Resource) null);
				}
			} catch (RepositoryException e) {
				throw new RDFHandlerException(e);
			}
		}
	
		@Override
		protected void reportDropNamed() throws RDFHandlerException {
			try {
				if (namedGraphs != null && namedGraphs.length > 0) {
					Resource[] contexts = new Resource[namedGraphs.length];
					for (int i=0;i<namedGraphs.length;i++) {
						contexts[i] = vf.createURI(namedGraphs[i]);
					}
					con.clear(contexts);
				}
			} catch (RepositoryException e) {
				throw new RDFHandlerException(e);
			}
		}
	
		@Override
		protected void reportDropAll() throws RDFHandlerException {
			try {
				con.clear();
			} catch (RepositoryException e) {
				throw new RDFHandlerException(e);
			}
		}
	}
}
