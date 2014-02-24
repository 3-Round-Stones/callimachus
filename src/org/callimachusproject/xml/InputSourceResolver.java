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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.trans.XPathException;

import org.apache.http.client.HttpClient;
import org.callimachusproject.client.HttpUriClient;
import org.callimachusproject.client.HttpUriEntity;
import org.callimachusproject.server.exceptions.NotFound;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class InputSourceResolver implements EntityResolver, URIResolver, ModuleURIResolver {
	private static final Pattern CHARSET = Pattern
			.compile("\\bcharset\\s*=\\s*([\\w-:]+)");
	private final HttpUriClient client;
	private final String accept;

	public InputSourceResolver(String accept, final HttpClient client) {
		this.accept = accept;
		this.client = new HttpUriClient() {
			protected HttpClient getDelegate() {
				return client;
			}
		};
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
	public StreamSource[] resolve(String moduleURI, String baseURI,
			String[] locations) throws XPathException {
		List<StreamSource> list = new ArrayList<StreamSource>();
		try {
			if (locations == null || locations.length == 0) {
				StreamSource resolved = resolve(moduleURI, baseURI);
				if (resolved == null) {
					XPathException se = new XPathException(
							"Cannot locate module for namespace " + moduleURI);
					se.setErrorCode("XQST0059");
					se.setIsStaticError(true);
					throw se;
				}
				list.add(resolved);
			} else {
				for (String location : locations) {
					StreamSource resolved = resolve(location, baseURI);
					if (resolved == null) {
						XPathException se = new XPathException(
								"Could not load module at " + location);
						se.setErrorCode("XQST0059");
						se.setIsStaticError(true);
						throw se;
					}
					list.add(resolved);
				}
			}
		} catch (XPathException e) {
			throw e;
		} catch (TransformerException e) {
			throw new XPathException(e);
		}
		return list.toArray(new StreamSource[list.size()]);
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
			if (entity == null)
				return null;
			InputStream in = entity.getContent();
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
