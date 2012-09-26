package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.xml.XdmNodeFactory;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;

public class Pipeline {
	private final XProcConfiguration config;
	private final XdmNodeFactory resolver;
	private final FluidBuilder fb = FluidFactory.getInstance().builder();
	private final String systemId;
	private final XdmNode pipeline;

	Pipeline(String systemId, XdmNodeFactory resolver, XProcConfiguration config) {
		assert systemId != null;
		this.systemId = systemId;
		this.config = config;
		this.resolver = resolver;
		this.pipeline = null;
	}

	Pipeline(InputStream in, String systemId, XdmNodeFactory resolver, XProcConfiguration config) throws SAXException, IOException {
		this.systemId = systemId;
		this.config = config;
		this.resolver = resolver;
		this.pipeline = resolver.parse(systemId, in);
	}

	Pipeline(Reader in, String systemId, XdmNodeFactory resolver, XProcConfiguration config) throws SAXException, IOException {
		this.systemId = systemId;
		this.config = config;
		this.resolver = resolver;
		this.pipeline = resolver.parse(systemId, in);
	}

	@Override
	public String toString() {
		if (systemId != null)
			return systemId;
		return pipeline.toString();
	}

	public String getSystemId() {
		return systemId;
	}

	public PipelineBuilder pipe() throws SAXException, IOException {
		return pipeSource(null);
	}

	public PipelineBuilder pipe(Object source, String systemId, Type type, String... media)
			throws SAXException, IOException, XProcException {
		return pipeReader(asReader(source, systemId, type, media), systemId);
	}

	public PipelineBuilder pipeStream(InputStream source, String systemId)
			throws SAXException, IOException, XProcException {
		return pipeSource(resolver.parse(systemId, source));
	}

	public PipelineBuilder pipeReader(Reader reader, String systemId) throws SAXException, IOException, XProcException {
		return pipeSource(resolver.parse(systemId, reader));
	}

	private PipelineBuilder pipeSource(XdmNode source) throws SAXException, XProcException, IOException {
		XProcRuntime runtime = new XProcRuntime(config);
		try {
			XPipeline xpipeline = runtime.use(resolvePipeline());
			if (source != null) {
				xpipeline.writeTo("source", source);
			}
			return new PipelineBuilder(runtime, resolver, resolver, xpipeline, systemId);
		} catch (SaxonApiException e) {
			throw new SAXException(e);
		}
	}

	private XdmNode resolvePipeline() throws IOException, SAXException {
		if (pipeline != null)
			return pipeline;
		return resolver.parse(systemId);
	}

	private Reader asReader(Object source, String systemId, Type type, String... media)
			throws IOException {
		try {
			return fb.consume(source, systemId, type, media).asReader();
		} catch (FluidException e) {
			throw new XProcException(e);
		}
	}

}
