package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;

import org.callimachusproject.xml.XdmNodeURIResolver;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcConfiguration;

public class PipelineFactory {
	private static final PipelineFactory instance = new PipelineFactory();

	public static PipelineFactory getInstance() {
		return instance;
	}

	private final XProcConfiguration config = new XProcConfiguration("he",
			false);
	private final XdmNodeURIResolver resolver;

	private PipelineFactory() {
		this.resolver = new XdmNodeURIResolver(config.getProcessor());
	}

	public Pipeline createPipeline(String systemId) throws SAXException,
			IOException {
		return new Pipeline(systemId, resolver, config);
	}

	public Pipeline createPipeline(InputStream in, String systemId)
			throws SAXException, IOException {
		return new Pipeline(in, systemId, resolver, config);
	}

}
