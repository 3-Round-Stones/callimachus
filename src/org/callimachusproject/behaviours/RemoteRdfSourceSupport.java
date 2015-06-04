package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Sparql;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.http.object.client.HttpUriResponse;
import org.openrdf.http.object.exceptions.BadGateway;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.model.URI;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserRegistry;

public abstract class RemoteRdfSourceSupport extends DatasourceSupport
		implements CalliObject {
	private static final ContentType SPARQL_UPDATE = ContentType.create("application/sparql-update");
	private static final byte[] INSERT_NOTHING = "INSERT DATA {}".getBytes(Consts.UTF_8);
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String PREFIX = "PREFIX calli:<" + CALLI + ">\n";
	private static final String QUERY_ASK = "?query=" + PercentCodec.encode("ASK{?s ?p ?o}");

	private final QueryParserRegistry reg = QueryParserRegistry.getInstance();

	public void updateEndpointInfo() throws IOException, OpenRDFException {
		URI endpoint = selectQueryEndpoint();
		if (endpoint == null)
			throw new BadRequest("No query endpoint is configured for this service");
		HttpUriClient client = this.getHttpClient();
		String url = endpoint.stringValue();
		HttpOptions options = new HttpOptions(url);
		HttpUriResponse resp = client.getAnyResponse(options);
		if (!resp.containsHeader("Server")) {
			setEndpointSoftware(null);
			return;
		}
		String server = resp.getFirstHeader("Server").getValue();
		setEndpointSoftware(server);
		String query = checkQueryEndpoint(url, server);
		if (!url.equals(query)) {
			setQueryEndpoint(vf.createURI(query));
		}
		verifyGatewayResponse(client.getAnyResponse(new HttpGet(query + QUERY_ASK)));
		URI updateEndpoint = selectUpdateEndpoint();
		if (updateEndpoint != null) {
			String update = checkUpdateEndpoint(updateEndpoint.stringValue(), server);
			if (!updateEndpoint.stringValue().equals(update)) {
				setUpdateEndpoint(vf.createURI(update));
			}
			HttpPost post = new HttpPost(update);
			post.setEntity(new ByteArrayEntity(INSERT_NOTHING, SPARQL_UPDATE));
			verifyGatewayResponse(client.getAnyResponse(post));
		}
	}

	/** The SPARQL endpoint for querying the RDF source */
	@Sparql(PREFIX + "SELECT ?endpoint {$this calli:queryEndpoint ?endpoint}")
	protected abstract URI selectQueryEndpoint();

	/** The SPARQL endpoint for updating the RDF source */
	@Sparql(PREFIX + "SELECT ?endpoint {$this calli:updateEndpoint ?endpoint}")
	protected abstract URI selectUpdateEndpoint();

	@Sparql(PREFIX
			+ "DELETE WHERE { $this calli:endpointSoftware ?software };\n"
			+ "INSERT { $this calli:endpointSoftware $server } WHERE {}\n")
	protected abstract void setEndpointSoftware(@Bind("server") String server);

	@Sparql(PREFIX + "DELETE { $this calli:queryEndpoint ?previously }\n"
			+ "INSERT { $this calli:queryEndpoint $endpoint }\n"
			+ "WHERE { OPTIONAL { $this calli:queryEndpoint ?previously } }")
	protected abstract void setQueryEndpoint(@Bind("endpoint") URI endpoint);

	@Sparql(PREFIX + "DELETE { $this calli:updateEndpoint ?previously }\n"
			+ "INSERT { $this calli:updateEndpoint $endpoint }\n"
			+ "WHERE { OPTIONAL { $this calli:updateEndpoint ?previously } }")
	protected abstract void setUpdateEndpoint(@Bind("endpoint") URI endpoint);

	@Override
	protected HttpUriResponse evaluateQuery(String[] defaultGraphUri,
			String[] namedGraphUri, String[] accept, String base, byte[] query)
			throws IOException, OpenRDFException {
		String loc = base == null ? this.toString() : resolve(base);
		if (!isFederatedSupported()) {
			QueryParser parser = reg.get(QueryLanguage.SPARQL).getParser();
			ParsedQuery parsed = parser.parseQuery(new String(query, "UTF-8"), loc);
			checkFederated(parsed.getTupleExpr());
		}
		URI endpoint = this.selectQueryEndpoint();
		if (endpoint == null)
			throw new InternalServerError("No query endpoint is configured for this service");
		String url = endpoint.stringValue();
		if (defaultGraphUri != null || namedGraphUri != null) {
			List<NameValuePair> qs = new ArrayList<NameValuePair>();
			if (defaultGraphUri != null) {
				for (String uri : defaultGraphUri) {
					qs.add(new BasicNameValuePair("default-graph-uri", uri));
				}
			}
			if (namedGraphUri != null) {
				for (String uri : namedGraphUri) {
					qs.add(new BasicNameValuePair("named-graph-uri", uri));
				}
			}
			url = url + "?" + URLEncodedUtils.format(qs, Consts.UTF_8);
		}
		HttpPost post = new HttpPost(url);
		if (accept != null) {
			for (String a : accept) {
				post.addHeader("Accept", a);
			}
		}
		post.setHeader("Content-Location", loc);
		post.setHeader("Content-Type", "application/sparql-query");
		post.setEntity(new ByteArrayEntity(query, ContentType
				.create("application/sparql-query")));
		return this.getHttpClient().getAnyResponse(post);
	}

	@Override
	protected HttpResponse executeUpdate(String[] usingGraphUri,
			String[] usingNamedGraphUri, String base, InputStream update)
			throws IOException, OpenRDFException {
		URI endpoint = this.selectUpdateEndpoint();
		if (endpoint == null)
			throw new InternalServerError("No update endpoint is configured for this service");
		String url = endpoint.stringValue();
		if (usingGraphUri != null || usingNamedGraphUri != null) {
			List<NameValuePair> qs = new ArrayList<NameValuePair>();
			if (usingGraphUri != null) {
				for (String uri : usingGraphUri) {
					qs.add(new BasicNameValuePair("using-graph-uri", uri));
				}
			}
			if (usingNamedGraphUri != null) {
				for (String uri : usingNamedGraphUri) {
					qs.add(new BasicNameValuePair("using-named-graph-uri", uri));
				}
			}
			url = url + "?" + URLEncodedUtils.format(qs, Consts.UTF_8);
		}
		HttpPost post = new HttpPost(url);
		String loc = base == null ? this.toString() : resolve(base);
		post.setHeader("Content-Location", loc);
		post.setHeader("Content-Type", "application/sparql-update");
		post.setEntity(new InputStreamEntity(update, SPARQL_UPDATE));
		return this.getHttpClient().getAnyResponse(post);
	}

	private void verifyGatewayResponse(HttpUriResponse resp) throws IOException {
		try {
		if (resp.getStatusLine().getStatusCode() >= 300) {
			ResponseException cause = ResponseException.create(resp);
			throw new BadGateway(cause.getMessage() + " from " + resp.getSystemId(), cause);
		}
		} finally {
			if (resp.getEntity() != null) {
				EntityUtils.consume(resp.getEntity());
			}
		}
	}

	private String checkQueryEndpoint(String url, String server) {
		if (server.contains("Apache-Coyote") && url.contains("/repositories/")) {
			return url.replaceAll("/openrdf-workbench/repositories/",
					"/openrdf-sesame/repositories/").replaceAll(
					"repositories/([^/]+).*$", "repositories/$1");
		} else if (server.contains("Stardog")) {
			return url.replaceAll("^(https?://[^/]+/[^/]+).*$", "$1/query");
		} else if (server.contains("Virtuoso")) {
			return url.replaceAll("^(https?://[^/]+)/?$", "$1/sparql");
		} else if (server.contains("Callimachus")) {
			return url;
		} else {
			logger.info("Unknown SPARQL endpoint server {}", server);
			return url;
		}
	}

	private String checkUpdateEndpoint(String url, String server) {
		if (server.contains("Apache-Coyote") && url.contains("/repositories/")) {
			return url.replaceAll("/openrdf-workbench/repositories/",
					"/openrdf-sesame/repositories/").replaceAll(
					"repositories/([^/]+).*$", "repositories/$1/statements");
		} else if (server.contains("Stardog")) {
			return url.replaceAll("^(https?://[^/]+/[^/]+).*$", "$1/update");
		} else if (server.contains("Virtuoso")) {
			return url.replaceAll("^(https?://[^/]+)/?$", "$1/sparql");
		} else if (server.contains("Callimachus")) {
			return url;
		} else {
			logger.info("Unknown SPARQL endpoint server {}", server);
			return url;
		}
	}
}
