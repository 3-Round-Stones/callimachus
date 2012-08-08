package org.callimachusproject.xml;

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.ref.Reference;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;

import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * Maintains a re-validating cache of {@link DOMSource}s.
 *  
 * @author James Leigh
 *
 */
public class XdmNodeURIResolver extends DedicatedURIResolver {
	private static final Map<String, Reference<URIResolver>> staticResolvers = newStaticResolver();

	private final XdmNodeResolver resolver;

	public XdmNodeURIResolver(Processor processor) {
		super(staticResolvers);
		resolver = new XdmNodeResolver(processor);
	}

	public XdmNode parse(String systemId, Reader in) throws SAXException, IOException {
		return resolver.parse(systemId, in);
	}

	public XdmNode parse(String systemId, InputStream in) throws SAXException, IOException {
		return resolver.parse(systemId, in);
	}

	public EntityResolver getEntityResolver() {
		return resolver.getEntityResolver();
	}

	public XdmNode resolve(String systemId) throws IOException, SAXException {
		return resolver.resolve(systemId);
	}

	@Override
	protected URIResolver createURIResolver() {
		return new EmptyURIResolver(new URIResolver() {
			
			@Override
			public Source resolve(String href, String base) throws TransformerException {
				try {
					XdmNode node = resolver.resolve(resolveURI(href, base));
					if (node == null)
						return null;
					return node.asSource();
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
