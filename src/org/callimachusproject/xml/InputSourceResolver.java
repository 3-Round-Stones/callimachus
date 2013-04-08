package org.callimachusproject.xml;

import info.aduna.net.ParsedURI;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.callimachusproject.client.HttpOriginClient;
import org.callimachusproject.client.HttpUriEntity;
import org.callimachusproject.server.exceptions.NotFound;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class InputSourceResolver implements EntityResolver, URIResolver {
	private static final Pattern CHARSET = Pattern
			.compile("\\bcharset\\s*=\\s*([\\w-:]+)");
	private final HttpOriginClient client;
	private final String accept;

	public InputSourceResolver(String systemId, String accept) {
		this.accept = accept;
		this.client = new HttpOriginClient(systemId);
	}

	@Override
	public StreamSource resolve(String href, String base) throws TransformerException {
		try {
			InputSource input = resolve(resolveURI(href, base));
			if (input == null)
				return null;
			InputStream in = input.getByteStream();
			if (in != null) {
				if (input.getSystemId() == null)
					return new StreamSource(in);
				return new StreamSource(in, input.getSystemId());
			}
			Reader reader = input.getCharacterStream();
			if (reader != null) {
				if (input.getSystemId() == null)
					return new StreamSource(reader);
				return new StreamSource(reader, input.getSystemId());
			}
			if (input.getSystemId() == null)
				return new StreamSource();
			return new StreamSource(input.getSystemId());
		} catch (IOException e) {
			throw new TransformerException(e);
		}
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId)
			throws IOException {
		return resolve(systemId);
	}

	public InputSource resolve(String systemId) throws IOException {
		if (systemId.startsWith("http:") || systemId.startsWith("https:"))
			return resolveHttp(systemId);
		return resolveURL(systemId);
	}

	private String resolveURI(String href, String base) {
		ParsedURI parsed = null;
		if (href != null) {
			parsed = new ParsedURI(href);
			if (parsed.isAbsolute())
				return href;
		}
		ParsedURI abs = null;
		if (base != null) {
			abs = new ParsedURI(base);
		}
		if (parsed != null) {
			if (abs == null) {
				abs = parsed;
			} else {
				abs = abs.resolve(parsed);
			}
		}
		return abs.toString();
	}

	/**
	 * returns null for 404 resources.
	 */
	private InputSource resolveHttp(String systemId) throws IOException {
		try {
			HttpUriEntity entity = client.getEntity(systemId, getAcceptHeader());
			InputStream in = entity == null ? null : entity.getContent();
			String type = entity.getContentType().getValue();
			systemId = entity.getSystemId();
			return create(type, in, systemId);
		} catch (NotFound e) {
			return null;
		}
	}

	/**
	 * returns null for 404 resources.
	 */
	private InputSource resolveURL(String systemId) throws IOException {
		URLConnection con = new URL(systemId).openConnection();
		con.addRequestProperty("Accept", getAcceptHeader());
		con.addRequestProperty("Accept-Encoding", "gzip");
		try {
			String base = con.getURL().toExternalForm();
			String type = con.getContentType();
			String encoding = con.getHeaderField("Content-Encoding");
			InputStream in = con.getInputStream();
			if (encoding != null && encoding.contains("gzip")) {
				in = new GZIPInputStream(in);
			}
			return create(type, in, base);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	private String getAcceptHeader() {
		return accept;
	}

	private InputSource create(String type, InputStream in, String systemId)
			throws UnsupportedEncodingException {
		Matcher m = CHARSET.matcher(type);
		if (m.find()) {
			Reader reader = new InputStreamReader(in, m.group(1));
			InputSource source = new InputSource(reader);
			if (systemId != null) {
				source.setSystemId(systemId);
			}
			return source;
		}
		InputSource source = new InputSource(in);
		if (systemId != null) {
			source.setSystemId(systemId);
		}
		return source;
	}

}