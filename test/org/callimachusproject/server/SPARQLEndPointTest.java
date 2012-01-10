package org.callimachusproject.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.NamedGraphSupport;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.openrdf.annotations.Matching;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultWriter;
import org.openrdf.query.resultio.BooleanQueryResultWriterRegistry;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;
import org.openrdf.query.resultio.TupleQueryResultWriterRegistry;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterRegistry;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class SPARQLEndPointTest extends MetadataServerTestCase {

	@Matching("/sparql")
	public interface SPARQLEndPoint {
	}

	public static abstract class SPARQLEndPointSupport implements
			SPARQLEndPoint, RDFObject {

		@type("message/http")
		@method("POST")
		public HttpResponse post(@type("*/*") Map<String, String> parameters)
				throws Exception {
			ObjectConnection con = getObjectConnection();
			ProtocolVersion ver = new ProtocolVersion("HTTP", 1, 1);
			HttpResponse resp = new BasicHttpResponse(ver, 200, "OK");
			Query query = con.prepareQuery(parameters.get("query"));
			if (query instanceof GraphQuery) {
				final GraphQueryResult result = ((GraphQuery) query).evaluate();
				resp.setEntity(new BasicHttpEntity() {
					public InputStream getContent()
							throws IllegalStateException {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						RDFWriter writer = RDFWriterRegistry.getInstance().get(
								RDFFormat.RDFXML).getWriter(out);
						try {
							writer.startRDF();
							while (result.hasNext()) {
								writer.handleStatement(result.next());
							}
							writer.endRDF();
							return new ByteArrayInputStream(out.toByteArray()) {
								@Override
								public void close() throws IOException {
									try {
										super.close();
									} finally {
										try {
											result.close();
										} catch (QueryEvaluationException e) {
											throw new AssertionError(e);
										}
									}
								}};
						} catch (Exception e) {
							throw new AssertionError(e);
						}
					}

					public Header getContentType() {
						return new BasicHeader("Content-Type",
								"application/rdf+xml");
					}

					public void writeTo(OutputStream outstream)
							throws IOException {
						try {
							super.writeTo(outstream);
						} finally {
							consumeContent();
						}
					}
				});
			} else if (query instanceof TupleQuery) {
				final TupleQueryResult result = ((TupleQuery) query).evaluate();
				resp.setEntity(new BasicHttpEntity() {
					public InputStream getContent()
							throws IllegalStateException {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						TupleQueryResultWriter writer = TupleQueryResultWriterRegistry
								.getInstance().get(
										TupleQueryResultFormat.SPARQL)
								.getWriter(out);
						try {
							writer.startQueryResult(result.getBindingNames());
							while (result.hasNext()) {
								writer.handleSolution(result.next());
							}
							writer.endQueryResult();
							return new ByteArrayInputStream(out.toByteArray()) {
								@Override
								public void close() throws IOException {
									try {
										super.close();
									} finally {
										try {
											result.close();
										} catch (QueryEvaluationException e) {
											throw new AssertionError(e);
										}
									}
								}};
						} catch (Exception e) {
							throw new AssertionError(e);
						}
					}

					public Header getContentType() {
						return new BasicHeader("Content-Type",
								"application/sparql-results+xml");
					}

					public void writeTo(OutputStream outstream)
							throws IOException {
						try {
							super.writeTo(outstream);
						} finally {
							consumeContent();
						}
					}
				});
			} else if (query instanceof BooleanQuery) {
				final boolean result = ((BooleanQuery) query).evaluate();
				resp.setEntity(new BasicHttpEntity() {
					public InputStream getContent()
							throws IllegalStateException {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						BooleanQueryResultWriter writer = BooleanQueryResultWriterRegistry
								.getInstance().get(
										BooleanQueryResultFormat.SPARQL)
								.getWriter(out);
						try {
							writer.write(result);
							return new ByteArrayInputStream(out.toByteArray());
						} catch (Exception e) {
							throw new AssertionError(e);
						}
					}

					public Header getContentType() {
						return new BasicHeader("Content-Type",
								"application/sparql-results+xml");
					}

					public void writeTo(OutputStream outstream)
							throws IOException {
						try {
							super.writeTo(outstream);
						} finally {
							consumeContent();
						}
					}
				});
			} else {
				throw new IllegalArgumentException();
			}
			return resp;
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(SPARQLEndPoint.class);
		config.addBehaviour(SPARQLEndPointSupport.class);
		config.addBehaviour(PUTSupport.class);
		config.addBehaviour(NamedGraphSupport.class);
		super.setUp();
		server.setEnvelopeType("message/http");
	}

	public void testGET_evaluateGraph() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#inSparql");
		Literal obj = vf
				.createLiteral("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
		URI NamedQuery = vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#NamedQuery");
		model.add(subj, RDF.TYPE, NamedQuery);
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		MultivaluedMap<String, String> map = new MultivaluedMapImpl();
		map.add("query", "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
		Model result = client.path("/sparql").post(Model.class, map);
		assertFalse(result.isEmpty());
	}

	public void testGET_evaluateTuple() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#inSparql");
		Literal obj = vf.createLiteral("SELECT ?s ?p ?o WHERE { ?s ?p ?o }");
		URI NamedQuery = vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#NamedQuery");
		model.add(subj, RDF.TYPE, NamedQuery);
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		MultivaluedMap<String, String> map = new MultivaluedMapImpl();
		map.add("query", "SELECT ?s ?p ?o WHERE { ?s ?p ?o }");
		String result = client.path("/sparql").post(String.class, map);
		assertTrue(result.startsWith("<?xml"));
	}
}
