package org.callimachusproject.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.callimachusproject.engine.impl.TermFactoryImpl;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.ChannelUtil;

public class WebResource {
	private static final Pattern LINK = Pattern.compile("<([^>]*)>(?:\\s*;\\s*anchor=\"([^\"]*)\"|\\s*;\\s*rel=\"([^\"]*)\"|\\s*;\\s*rel=([a-z0-9\\.\\-]*)|\\s*;\\s*type=\"([^\"]*)\"|\\s*;\\s*type=([a-zA-z0-9\\.\\-\\+]*))*");
	private final String uri;

	protected WebResource(String uri) {
		this.uri = uri;
	}

	public WebResource ref(String reference) throws IOException {
		return new WebResource(TermFactory.newInstance(uri).resolve(reference));
	}

	public WebResource link(String rel, String... types) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri)
				.openConnection();
		con.setRequestMethod("OPTIONS");
		Assert.assertEquals(con.getResponseMessage(), 204,
				con.getResponseCode());
		String header = con.getHeaderField("Link");
		Assert.assertNotNull(header);
		Matcher m = LINK.matcher(header);
		while (m.find()) {
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
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(uri).append("?").append(rel);
		sb.append(">; rel=\"").append(rel).append("\"; type=\"");
		for (String type : types) {
			sb.append(type).append(' ');
		}
		sb.setLength(sb.length() - 1);
		sb.append("\"");
		Assert.assertEquals(sb.toString(), header);
		return null;
	}

	public WebResource getAppCollection() throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("ACCEPT", "application/atom+xml");
		Assert.assertEquals(con.getResponseMessage(), 203, con.getResponseCode());
		InputStream stream = con.getInputStream();
		String text = new java.util.Scanner(stream).useDelimiter("\\A").next();
		String result = getQuoteAfter("<app:collection", text);
		return ref(result);
	}

	public WebResource create(String type, byte[] body) throws IOException {
		return create(null, type, body);
	}

	public WebResource create(String slug, String type, byte[] body) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("POST");
		if (slug != null) {
			con.setRequestProperty("Slug", slug);
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

	public WebResource createPURL(String slug, String property, String target) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("INSERT DATA {\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a calli:PURL, </callimachus/PURL>;\n");
		sb.append("rdfs:label \"").append(slug).append("\" ;\n");
		sb.append("calli:").append(property).append(" \"\"\"").append(target).append("\"\"\"\n");
		sb.append("}");
		return link("describedby").create("application/sparql-update", sb.toString().getBytes("UTF-8"));
	}

	public WebResource createFolder(String slug) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("INSERT DATA {\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a calli:Folder, </callimachus/Folder>;\n");
		sb.append("rdfs:label \"").append(slug).append("\"\n");
		sb.append("}");
		return link("describedby").create("application/sparql-update", sb.toString().getBytes("UTF-8"));
	}

	public String getRedirectLocation() throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("HEAD");
		int code = con.getResponseCode();
		if (!(code == 301 || code == 303 || code == 302 || code == 307 || code == 308))
			Assert.assertEquals(con.getResponseMessage(), 302, code);
		return con.getHeaderField("Location");
	}

	public int headCode() throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("HEAD");
		return con.getResponseCode();
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
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("PUT");
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
		DefaultHttpClient client = new DefaultHttpClient();
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
		client.getCredentialsProvider().setCredentials(
				new AuthScope(host, port), credentials);
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
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("DELETE");
		int code = con.getResponseCode();
		if (code != 200 && code != 204) {
			Assert.assertEquals(con.getResponseMessage(), 204, code);
		}
	}

	public WebResource search(String searchTerm) throws Exception {
		String text = new String(link("search", "application/opensearchdescription+xml").get("application/opensearchdescription+xml"));
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

	public void update(String sparql) throws Exception {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/sparql-update");
		con.setDoOutput(true);
		OutputStream out = con.getOutputStream();
		try {
			out.write(sparql.getBytes("UTF-8"));
		} finally {
			out.close();
		}
		Assert.assertEquals(con.getResponseMessage(), 204, con.getResponseCode());
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

}
