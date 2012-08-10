package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;

import net.sf.saxon.s9api.Processor;

import org.callimachusproject.xml.XdmNodeURIResolver;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;

public class PipelineFactory {
	private static final PipelineFactory instance;
	static {
		try {
			instance = new PipelineFactory();
		} catch (XProcException e) {
			throw new AssertionError(e);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	public static PipelineFactory getInstance() {
		return instance;
	}

	private final XProcConfiguration config;
	private final XdmNodeURIResolver resolver;

	private PipelineFactory() throws IOException, XProcException {
		config = new XProcConfiguration("he", false);
		Processor processor = config.getProcessor();
		this.resolver = new XdmNodeURIResolver(processor);
    	ClassLoader cl = getClass().getClassLoader();
		Enumeration<URL> resources = cl.getResources("META-INF/xmlcalabash.xml");
		while (resources.hasMoreElements()) {
	        try {
	            URL puri = resources.nextElement();
				InputStream in = puri.openStream();
	            config.parse(resolver.parse(puri.toExternalForm(), in));
	        } catch (SAXException sae) {
	            throw new XProcException(sae);
	        }
		}
	}

	public Pipeline createPipeline(String systemId) throws SAXException,
			IOException {
		return new Pipeline(systemId, resolver, config);
	}

	public Pipeline createPipeline(InputStream in, String systemId)
			throws SAXException, IOException {
		return new Pipeline(in, systemId, resolver, config);
	}

	public Pipeline createPipeline(Reader reader, String systemId)
			throws SAXException, IOException {
		return new Pipeline(reader, systemId, resolver, config);
	}

}
