package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicNameValuePair;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Sparql;
import org.openrdf.http.object.client.HttpUriResponse;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserRegistry;
import org.openrdf.repository.object.RDFObject;

public abstract class RemoteRdfSourceSupport extends DatasourceSupport
		implements CalliObject {
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String PREFIX = "PREFIX calli:<" + CALLI + ">\n";

	private final QueryParserRegistry reg = QueryParserRegistry.getInstance();

	/** The SPARQL endpoint for querying the RDF source */
	@Sparql(PREFIX + "SELECT ?endpoint {$this calli:queryEndpoint ?endpoint}")
	protected abstract RDFObject selectQueryEndpoint();

	/** The SPARQL endpoint for updating the RDF source */
	@Sparql(PREFIX + "SELECT ?endpoint {$this calli:updateEndpoint ?endpoint}")
	protected abstract RDFObject selectUpdateEndpoint();

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
		RDFObject endpoint = this.selectQueryEndpoint();
		if (endpoint == null)
			throw new InternalServerError("No query endpoint is configured for this service");
		String url = endpoint.toString();
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
		RDFObject endpoint = this.selectUpdateEndpoint();
		if (endpoint == null)
			throw new InternalServerError("No update endpoint is configured for this service");
		String url = endpoint.toString();
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
		post.setEntity(new InputStreamEntity(update, ContentType
				.create("application/sparql-update")));
		return this.getHttpClient().getAnyResponse(post);
	}
}
