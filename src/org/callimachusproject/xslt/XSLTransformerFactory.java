package org.callimachusproject.xslt;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.transform.TransformerFactory;

public class XSLTransformerFactory {
	private static final XSLTransformerFactory instance = new XSLTransformerFactory();

	public static XSLTransformerFactory getInstance() {
		return instance;
	}

	private final TransformerFactory tfactory = new CachedTransformerFactory();

	XSLTransformerFactory() {
		super();
	}

	public XSLTransformer createTransformer() {
		return new XSLTransformer(null, tfactory);
	}

	public XSLTransformer createTransformer(String systemId) {
		return new XSLTransformer(systemId, tfactory);
	}

	public XSLTransformer createTransformer(InputStream in, String systemId) {
		return new XSLTransformer(in, systemId, tfactory);
	}

	public XSLTransformer createTransformer(Reader r, String systemId) {
		return new XSLTransformer(r, systemId, tfactory);
	}

}
