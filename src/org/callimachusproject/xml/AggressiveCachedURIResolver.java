package org.callimachusproject.xml;

import info.aduna.net.ParsedURI;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import net.sf.saxon.om.DocumentInfo;

/**
 * Aggressive caching of {@link DOMSource}s; intended to be used with a single
 * operation.
 * 
 * @author James Leigh
 * 
 */
public class AggressiveCachedURIResolver implements URIResolver {
	private final ParsedURI systemId;
	private final URIResolver delegate;
	private final Map<String, Source> sources = new HashMap<String, Source>();

	public AggressiveCachedURIResolver(String systemId, URIResolver delegate) {
		this.systemId = systemId == null ? null : new ParsedURI(systemId);
		this.delegate = delegate;
	}

	public synchronized Source resolve(String href, String base)
			throws TransformerException {
		String url = resolveURI(href, base);
		if (sources.containsKey(url))
			return sources.get(url);
		Source source = delegate.resolve(url, url);
		if (source instanceof DOMSource) {
			sources.put(url, (DOMSource) source);
		} else if (source instanceof DocumentInfo) {
			sources.put(url, (DocumentInfo) source);
		}
		return source;
	}

	private String resolveURI(String href, String base) {
		ParsedURI parsed = null;
		if (href != null) {
			parsed = new ParsedURI(href);
			if (parsed.isAbsolute())
				return href;
		}
		ParsedURI abs = systemId;
		if (base != null) {
			if (abs == null) {
				abs = new ParsedURI(base);
			} else {
				abs = abs.resolve(base);
			}
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

}
