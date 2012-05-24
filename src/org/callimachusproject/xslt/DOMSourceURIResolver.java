package org.callimachusproject.xslt;

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

public class DOMSourceURIResolver implements URIResolver {
	private static final DocumentFactory df = DocumentFactory.newInstance();
	private static final Map<String, Reference<DOMSourceResolver>> staticResolvers = new LinkedHashMap<String, Reference<DOMSourceResolver>>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1362917757653811798L;

		protected boolean removeEldestEntry(
				Map.Entry<String, Reference<DOMSourceResolver>> eldest) {
			return size() > 1024;
		}
	};

	private final ParsedURI systemId;
	private final Map<String, Reference<DOMSourceResolver>> instanceResolvers = new LinkedHashMap<String, Reference<DOMSourceResolver>>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1362917757653811798L;

		protected boolean removeEldestEntry(
				Map.Entry<String, Reference<DOMSourceResolver>> eldest) {
			return size() > 16;
		}
	};

	public DOMSourceURIResolver(String systemId) {
		this.systemId = systemId == null ? null : new ParsedURI(systemId);
	}

	public DOMSource resolve(String href, String base)
			throws TransformerException {
		return resolve(resolveURI(href, base));
	}

	public DOMSource resolve(String url) throws TransformerException {
		try {
			DOMSourceResolver resolver = getResolver(url);
			DOMSource source = resolver.resolve(url);
			if (source == null) {
				// use empty node-set
				Document doc = df.newDocument();
				return new DOMSource(doc, url);
			}
			return source;
		} catch (IOException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		}
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

	private DOMSourceResolver getResolver(String url) {
		synchronized (instanceResolvers) {
			Iterator<Entry<String, Reference<DOMSourceResolver>>> iter;
			iter = instanceResolvers.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Reference<DOMSourceResolver>> e;
				e = iter.next();
				if (e.getValue().get() == null) {
					iter.remove();
				}
			}
			Reference<DOMSourceResolver> ref = instanceResolvers.get(url);
			if (ref != null) {
				DOMSourceResolver resolver = ref.get();
				if (resolver != null)
					return resolver;
			}
			DOMSourceResolver resolver = getStaticResolver(url);
			instanceResolvers.put(url, new SoftReference<DOMSourceResolver>(
					resolver));
			return resolver;
		}

	}

	private DOMSourceResolver getStaticResolver(String url) {
		synchronized (staticResolvers) {
			Iterator<Entry<String, Reference<DOMSourceResolver>>> iter;
			iter = staticResolvers.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Reference<DOMSourceResolver>> e;
				e = iter.next();
				if (e.getValue().get() == null) {
					iter.remove();
				}
			}
			Reference<DOMSourceResolver> ref = staticResolvers.get(url);
			if (ref != null) {
				DOMSourceResolver resolver = ref.get();
				if (resolver != null)
					return resolver;
			}
			DOMSourceResolver resolver = new DOMSourceResolver();
			staticResolvers.put(url, new SoftReference<DOMSourceResolver>(
					resolver));
			return resolver;
		}
	}

}
