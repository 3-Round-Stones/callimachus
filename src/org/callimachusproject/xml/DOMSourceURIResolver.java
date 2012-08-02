package org.callimachusproject.xml;

import info.aduna.net.ParsedURI;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.SAXException;

/**
 * Maintains a re-validating cache of {@link DOMSource}s.
 *  
 * @author James Leigh
 *
 */
public class DOMSourceURIResolver extends DedicatedURIResolver {

	@Override
	protected URIResolver createURIResolver() {
		final DOMSourceResolver resolver = new DOMSourceResolver();
		return new EmptyURIResolver(new URIResolver() {
			
			@Override
			public Source resolve(String href, String base) throws TransformerException {
				try {
					return resolver.resolve(resolveURI(href, base));
				} catch (IOException e) {
					throw new TransformerException(e);
				} catch (SAXException e) {
					throw new TransformerException(e);
				}
			}
		});
	}

	String resolveURI(String href, String base) {
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
}
