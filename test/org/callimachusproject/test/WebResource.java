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
package org.callimachusproject.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.callimachusproject.engine.impl.TermFactoryImpl;
import org.callimachusproject.engine.model.TermFactory;
import org.openrdf.http.object.io.ChannelUtil;

public class WebResource {
	private static final Pattern LINK = Pattern.compile("<([^>]*)>(?:\\s*;\\s*anchor=\"([^\"]*)\"|\\s*;\\s*(?:rel|rev)=\"([^\"]*)\"|\\s*;\\s*rel=([a-z0-9\\.\\-]*)|\\s*;\\s*type=\"([^\"]*)\"|\\s*;\\s*type=([a-zA-z0-9\\.\\-\\+]*))*");
	private final String uri;

	public WebResource(String uri) {
		this.uri = uri;
	}

	public WebResource ref(String reference) throws IOException {
		return new WebResource(TermFactory.newInstance(uri).resolve(reference));
	}

	public WebResource rel(String rel, String... types) throws IOException {
		return findLink(rel, false, types);
	}

	public WebResource rev(String rev, String... types) throws IOException {
		return findLink(rev, true, types);
	}

	public WebResource getAppCollection() throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("ACCEPT", "application/atom+xml");
		Assert.assertEquals(con.getResponseMessage(), 203, con.getResponseCode());
		InputStream stream = con.getInputStream();
		java.util.Scanner scanner = new java.util.Scanner(stream);
		try {
			String text = scanner.useDelimiter("\\A").next();
			String result = getQuoteAfter("<app:collection", text);
			return ref(result);
		} finally {
			scanner.close();
		}
	}

	public WebResource create(String type, byte[] body) throws IOException {
		Map<String, String> headers = Collections.emptyMap();
		return create(headers, type, body);
	}

	public WebResource create(String slug, String type, byte[] body) throws IOException {
		return create(Collections.singletonMap("Slug", slug), type, body);
	}

	public WebResource create(Map<String, String> headers, String type, byte[] body) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("POST");
		for (Map.Entry<String, String> e : headers.entrySet()) {
			con.setRequestProperty(e.getKey(), e.getValue());
		}
		con.setRequestProperty("Content-Type", type);
		con.setDoOutput(true);
		OutputStream out = con.getOutputStream();
		try {
			out.write(body);
		} finally {
			out.close();
		}
		Assert.assertEquals(con.getResponseMessage(), 201, con.getResponseCode());
		String header = con.getHeaderField("Location");
		Assert.assertNotNull(header);
		return ref(header);
	}

	public WebResource createPurl(String slug, String property, String target) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a calli:Purl;\n");
		sb.append("rdfs:label \"").append(slug).append("\" ;\n");
		sb.append("calli:").append(property).append(" \"\"\"").append(target).append("\"\"\".\n");
		return rel("describedby").create("text/turtle", sb.toString().getBytes("UTF-8")).rev("describedby");
	}

	public WebResource createRedirect(String slug, String property, String target) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a calli:Redirect;\n");
		sb.append("rdfs:label \"").append(slug).append("\" ;\n");
		sb.append("calli:").append(property).append(" \"\"\"").append(target).append("\"\"\".\n");
		return rel("describedby").create("text/turtle", sb.toString().getBytes("UTF-8")).rev("describedby");
	}

	public WebResource createProxy(String slug, String property, String target) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a calli:Proxy;\n");
		sb.append("rdfs:label \"").append(slug).append("\" ;\n");
		sb.append("calli:").append(property).append(" \"\"\"").append(target).append("\"\"\".\n");
		return rel("describedby").create("text/turtle", sb.toString().getBytes("UTF-8")).rev("describedby");
	}

	public WebResource createRewriteRule(String slug, String property, String target) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a calli:RewriteRule;\n");
		sb.append("rdfs:label \"").append(slug).append("\" ;\n");
		sb.append("calli:").append(property).append(" \"\"\"").append(target).append("\"\"\".\n");
		return rel("describedby").create("text/turtle", sb.toString().getBytes("UTF-8")).rev("describedby");
	}

	public WebResource createFolder(String slug) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a calli:Folder;\n");
		sb.append("rdfs:label \"").append(slug).append("\".\n");
		return rel("describedby").create("text/turtle", sb.toString().getBytes("UTF-8")).rev("describedby");
	}

	public WebResource createRdfDatasource(String slug) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a sd:Service;\n");
		sb.append("rdfs:label \"").append(slug).append("\";\n");
		sb.append("sd:endpoint <").append(slug).append(">;\n");
		sb.append("sd:supportedLanguage sd:SPARQL11Query;\n");
		sb.append("sd:supportedLanguage sd:SPARQL11Update;\n");
		sb.append("sd:feature sd:UnionDefaultGraph;\n");
		sb.append("sd:feature sd:BasicFederatedQuery;\n");
		sb.append("sd:inputFormat <http://www.w3.org/ns/formats/RDF_XML>;\n");
		sb.append("sd:inputFormat <http://www.w3.org/ns/formats/Turtle>;\n");
		sb.append("sd:inputFormat <http://www.w3.org/ns/formats/JSON-LD>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/RDF_XML>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/Turtle>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/JSON-LD>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_XML>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_JSON>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_CSV>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_TSV>.\n");
		return rel("describedby").create("text/turtle", sb.toString().getBytes("UTF-8")).rev("describedby");
	}

	public WebResource createRemoteRdfSource(String slug, String endpoint) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a sd:Service, calli:RemoteRdfSource;\n");
		sb.append("rdfs:label \"").append(slug).append("\";\n");
		sb.append("calli:queryEndpoint <").append(endpoint).append(">;\n");
		sb.append("calli:updateEndpoint <").append(endpoint).append(">;\n");
		sb.append("sd:endpoint <").append(slug).append(">;\n");
		sb.append("sd:supportedLanguage sd:SPARQL11Query;\n");
		sb.append("sd:supportedLanguage sd:SPARQL11Update;\n");
		sb.append("sd:feature sd:UnionDefaultGraph;\n");
		sb.append("sd:feature sd:BasicFederatedQuery;\n");
		sb.append("sd:inputFormat <http://www.w3.org/ns/formats/RDF_XML>;\n");
		sb.append("sd:inputFormat <http://www.w3.org/ns/formats/Turtle>;\n");
		sb.append("sd:inputFormat <http://www.w3.org/ns/formats/JSON-LD>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/RDF_XML>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/Turtle>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/JSON-LD>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_XML>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_JSON>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_CSV>;\n");
		sb.append("sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_TSV>.\n");
		return rel("describedby").create("text/turtle", sb.toString().getBytes("UTF-8")).rev("describedby");
	}

	public WebResource getRedirectTarget() throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("HEAD");
		int code = con.getResponseCode();
		if (!(code == 301 || code == 303 || code == 302 || code == 307 || code == 308))
			Assert.assertEquals(con.getResponseMessage(), 302, code);
		return ref(con.getHeaderField("Location"));
	}

	public int headCode() throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("HEAD");
		return con.getResponseCode();
	}

	public String headETag(String accept) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("HEAD");
		con.setRequestProperty("Accept", accept);
		return con.getHeaderField("ETag");
	}

	public byte[] get(String type) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", type);
		con.setRequestProperty("Accept-Encoding", "gzip");
		Assert.assertEquals(con.getResponseMessage(), 200, con.getResponseCode());
		InputStream in = con.getInputStream();
		try {
			if ("gzip".equals(con.getHeaderField("Content-Encoding"))) {
				in = new GZIPInputStream(in);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChannelUtil.transfer(in, out);
			return out.toByteArray();
		} finally {
			in.close();
		}
	}

	public byte[] post(String type, byte[] body, String accept) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Accept", accept);
		con.setRequestProperty("Accept-Encoding", "gzip");
		con.setRequestProperty("Content-Type", type);
		con.setDoOutput(true);
		OutputStream req = con.getOutputStream();
		try {
			req.write(body);
		} finally {
			req.close();
		}
		Assert.assertEquals(con.getResponseMessage(), 200, con.getResponseCode());
		InputStream in = con.getInputStream();
		try {
			if ("gzip".equals(con.getHeaderField("Content-Encoding"))) {
				in = new GZIPInputStream(in);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChannelUtil.transfer(in, out);
			return out.toByteArray();
		} finally {
			in.close();
		}
	}

	public void post() throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(false);
		Assert.assertEquals(con.getResponseMessage(), 204, con.getResponseCode());
	}

	public void post(String type, byte[] body) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", type);
		con.setDoOutput(true);
		OutputStream req = con.getOutputStream();
		try {
			req.write(body);
		} finally {
			req.close();
		}
		Assert.assertEquals(con.getResponseMessage(), 204, con.getResponseCode());
	}

	public void put(String type, byte[] body) throws IOException {
		putIf("*", type, body);
	}

	public void putIf(String match, String type, byte[] body) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("PUT");
		if (match != null) {
			con.setRequestProperty("If-Match", match);
		}
		con.setRequestProperty("Content-Type", type);
		con.setDoOutput(true);
		OutputStream out = con.getOutputStream();
		try {
			out.write(body);
		} finally {
			out.close();
		}
		Assert.assertEquals(con.getResponseMessage(), 204, con.getResponseCode());
	}

	public void patch(String type, byte[] body) throws IOException {
		ByteArrayEntity entity = new ByteArrayEntity(body);
		entity.setContentType(type);
		HttpPatch req = new HttpPatch(uri);
		req.setEntity(entity);
		URL url = req.getURI().toURL();
		int port = url.getPort();
		String host = url.getHost();
		PasswordAuthentication passAuth = Authenticator
				.requestPasswordAuthentication(url.getHost(),
						InetAddress.getByName(url.getHost()), port,
						url.getProtocol(), "", "digest", url,
						RequestorType.SERVER);
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
				passAuth.getUserName(), new String(passAuth.getPassword()));
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(host, port), credentials);
		HttpClient client = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider).build();
		client.execute(req, new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				StatusLine status = response.getStatusLine();
				int code = status.getStatusCode();
				if (code != 200 && code != 204) {
					Assert.assertEquals(status.getReasonPhrase(), 204, code);
				}
				return null;
			}
		});
	}

	public void delete() throws IOException {
		deleteIf(null);
	}

	public void deleteIf(String match) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("DELETE");
		if (match != null) {
			con.setRequestProperty("If-Match", match);
		}
		int code = con.getResponseCode();
		if (code != 200 && code != 204) {
			Assert.assertEquals(con.getResponseMessage(), 204, code);
		}
	}

	public WebResource search(String searchTerm) throws Exception {
		String text = new String(rel("search", "application/opensearchdescription+xml").get("application/opensearchdescription+xml"));
		int pos = text.indexOf("template");
		int start = text.indexOf("\"", pos);
		int stop = text.indexOf("\"", start + 1);
		String templateURL = text.substring(start + 1, stop);
		
		int before = templateURL.indexOf("{");
		int after = templateURL.indexOf("}");
		String firstPart = templateURL.substring(0, before);
		String secondPart = templateURL.substring(after+1);
		String fullURL = firstPart + searchTerm + secondPart;
		return ref(fullURL);
	}

	public WebResource sparqlEndpoint() throws Exception {
		String _void = new String(ref("/.well-known/void").get("application/rdf+xml"));
		Pattern regex = Pattern.compile("\\bsparqlEndpoint\\b[^>]*\\brdf:resource=\"([^\"]*)\"\\s*/>");
		Matcher m = regex.matcher(_void);
		Assert.assertTrue(m.find());
		return ref(m.group(1));
	}

	public WebResource query(String sparql) throws Exception {
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		return ref("?query=" + encoded);
	}

	@Override
	public String toString() {
		return uri;
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WebResource other = (WebResource) obj;
		return uri.equals(other.uri);
	}

	private String getQuoteAfter(String token, String text) {
		int pos = text.indexOf(token);
		if (pos <= 0)
			return null;
		int start = text.indexOf("\"", pos);
		int stop = text.indexOf("\"", start + 1);
		return text.substring(start + 1, stop).replace("&amp;", "&");
	}

	private WebResource findLink(String rel, boolean rev, String... types)
			throws IOException, MalformedURLException, ProtocolException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri)
				.openConnection();
		con.setRequestMethod("OPTIONS");
		Assert.assertEquals(con.getResponseMessage(), 204,
				con.getResponseCode());
		for (Map.Entry<String, List<String>> e : con.getHeaderFields().entrySet()) {
			if (!"Link".equalsIgnoreCase(e.getKey()))
				continue;
			for (String header : e.getValue()) {
				Assert.assertNotNull(header);
				Matcher m = LINK.matcher(header);
				while (m.find()) {
					if (rev && !header.contains("rev="))
						continue;
					String href = m.group(1);
					String a = m.group(2);
					String r = m.group(3) != null ? m.group(3) : m.group(4);
					String t = m.group(5) != null ? m.group(5) : m.group(6);
					if (a != null
							&& !TermFactoryImpl.newInstance(uri).resolve(a).equals(uri))
						continue;
					if (!rel.equals(r))
						continue;
					if (types.length == 0 || t == null)
						return ref(href);
					for (String type : types) {
						for (String t1 : t.split("\\s+")) {
							if (t1.length() > 0 && t1.startsWith(type)) {
								return ref(href);
							}
						}
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(uri).append("?").append(rel);
		sb.append(">");
		if (rev) {
			sb.append("; rev=\"");
		} else {
			sb.append("; rel=\"");
		}
		sb.append(rel).append("\"; type=\"");
		for (String type : types) {
			sb.append(type).append(' ');
		}
		sb.setLength(sb.length() - 1);
		sb.append("\"");
		Assert.assertEquals(sb.toString(), con.getHeaderField("Link"));
		return null;
	}

}
