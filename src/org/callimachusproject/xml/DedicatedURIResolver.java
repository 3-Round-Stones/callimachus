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

	private final Map<String, Reference<URIResolver>> resolverPool = new LinkedHashMap<String, Reference<URIResolver>>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1362917757653811798L;

		protected boolean removeEldestEntry(
				Map.Entry<String, Reference<URIResolver>> eldest) {
			return size() > 1024;
		}
	};
	private final Map<String, Reference<URIResolver>> resolverPuddle = new LinkedHashMap<String, Reference<URIResolver>>(
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
		URIResolver resolver = get(url, resolverPuddle);
		if (resolver != null)
			return resolver;
		resolver = getSharedResolver(url);
		purge(resolverPuddle);
		put(url, resolver, resolverPuddle);
		return resolver;
	}

	private URIResolver getSharedResolver(String url) {
		URIResolver resolver = get(url, resolverPool);
		if (resolver != null)
			return resolver;
		resolver = createURIResolver();
		synchronized (resolverPool) {
			URIResolver other = get(url, resolverPool);
			if (other != null)
				return other;
			purge(resolverPool);
			put(url, resolver, resolverPool);
			return resolver;
		}
	}

	private void purge(Map<String, Reference<URIResolver>> resolvers) {
		synchronized (resolvers) {
			Iterator<Entry<String, Reference<URIResolver>>> iter;
			iter = resolvers.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Reference<URIResolver>> e;
				e = iter.next();
				if (e.getValue().get() == null) {
					iter.remove();
				}
			}
		}
	}

	private URIResolver get(String url,
			Map<String, Reference<URIResolver>> resolvers) {
		synchronized (resolvers) {
			Reference<URIResolver> ref = resolvers.get(url);
			if (ref == null)
				return null;
			return ref.get();
		}
	}

	private void put(String url, URIResolver resolver,
			Map<String, Reference<URIResolver>> resolvers) {
		synchronized (resolvers) {
			resolvers.put(url, new SoftReference<URIResolver>(resolver));
		}
	}

	protected abstract URIResolver createURIResolver();

}
