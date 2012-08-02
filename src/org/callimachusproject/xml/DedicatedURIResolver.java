package org.callimachusproject.xml;

import info.aduna.net.ParsedURI;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

/**
 * Maintains a weak set of dedicated {@link URIResolver}s.
 *  
 * @author James Leigh
 *
 */
public abstract class DedicatedURIResolver implements URIResolver {
	private static final Map<String, Reference<URIResolver>> staticResolvers = new LinkedHashMap<String, Reference<URIResolver>>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1362917757653811798L;

		protected boolean removeEldestEntry(
				Map.Entry<String, Reference<URIResolver>> eldest) {
			return size() > 1024;
		}
	};

	private final Map<String, Reference<URIResolver>> instanceResolvers = new LinkedHashMap<String, Reference<URIResolver>>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1362917757653811798L;

		protected boolean removeEldestEntry(
				Map.Entry<String, Reference<URIResolver>> eldest) {
			return size() > 16;
		}
	};

	public Source resolve(String href, String base)
			throws TransformerException {
		return resolve(resolveURI(href, base));
	}

	private Source resolve(String url) throws TransformerException {
		URIResolver resolver = getResolver(url);
		return resolver.resolve(url, url);
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

	private URIResolver getResolver(String url) {
		synchronized (instanceResolvers) {
			Iterator<Entry<String, Reference<URIResolver>>> iter;
			iter = instanceResolvers.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Reference<URIResolver>> e;
				e = iter.next();
				if (e.getValue().get() == null) {
					iter.remove();
				}
			}
			Reference<URIResolver> ref = instanceResolvers.get(url);
			if (ref != null) {
				URIResolver resolver = ref.get();
				if (resolver != null)
					return resolver;
			}
			URIResolver resolver = getStaticResolver(url);
			instanceResolvers.put(url, new SoftReference<URIResolver>(
					resolver));
			return resolver;
		}

	}

	private URIResolver getStaticResolver(String url) {
		synchronized (staticResolvers) {
			Iterator<Entry<String, Reference<URIResolver>>> iter;
			iter = staticResolvers.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Reference<URIResolver>> e;
				e = iter.next();
				if (e.getValue().get() == null) {
					iter.remove();
				}
			}
			Reference<URIResolver> ref = staticResolvers.get(url);
			if (ref != null) {
				URIResolver resolver = ref.get();
				if (resolver != null)
					return resolver;
			}
			URIResolver resolver = createURIResolver();
			staticResolvers.put(url, new SoftReference<URIResolver>(
					resolver));
			return resolver;
		}
	}

	protected abstract URIResolver createURIResolver();

}
