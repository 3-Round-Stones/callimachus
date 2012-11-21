package org.callimachusproject.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import junit.framework.Assert;

import org.callimachusproject.engine.impl.TermFactoryImpl;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.server.util.ChannelUtil;

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

	public byte[] get(String type) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", type);
		con.setRequestProperty("Accept-Encoding", "gzip");
		Assert.assertEquals(con.getResponseMessage(), 200, con.getResponseCode());
		InputStream in = con.getInputStream();
		if ("gzip".equals(con.getHeaderField("Content-Encoding"))) {
			in = new GZIPInputStream(in);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ChannelUtil.transfer(in, out);
		return out.toByteArray();
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

	public void delete() throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(uri).openConnection();
		con.setRequestMethod("DELETE");
		Assert.assertEquals(con.getResponseMessage(), 204, con.getResponseCode());
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
		int start = text.indexOf("\"", pos);
		int stop = text.indexOf("\"", start + 1);
		return text.substring(start + 1, stop).replace("&amp;", "&");
	}

}
