package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;

import org.callimachusproject.xml.XdmNodeFactory;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;

public class PipelineFactory {

	public static PipelineFactory newInstance() throws IOException {
		return new PipelineFactory();
	}

	private final XProcConfiguration config;
	private final XdmNodeFactory resolver;

	private PipelineFactory() throws IOException {
		this.config = new XProcConfiguration("he", false);
		this.resolver = new XdmNodeFactory(config.getProcessor());
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

	public Pipeline createPipeline(String systemId) {
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
