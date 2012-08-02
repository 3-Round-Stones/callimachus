package org.callimachusproject.xml;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

/**
 * Replaces a null source with an empty XML document.
 * 
 * @author James Leigh
 *
 */
public class EmptyURIResolver implements URIResolver {
	private static final DocumentFactory df = DocumentFactory.newInstance();
	private final URIResolver delegate;

	public EmptyURIResolver(URIResolver delegate) {
		this.delegate = delegate;
	}

	@Override
	public Source resolve(String href, String base) throws TransformerException {
		Source source = delegate.resolve(href, base);
		if (source == null) {
			try {
				// use empty node-set
				Document doc = df.newDocument();
				return new DOMSource(doc);
			} catch (ParserConfigurationException e) {
				throw new TransformerException(e);
			}
		}
		return source;
	}

}
