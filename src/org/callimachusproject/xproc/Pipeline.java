package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.xml.XdmNodeURIResolver;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;

public class Pipeline {
	private final XProcConfiguration config;
	private final XdmNodeURIResolver resolver;
	private final FluidBuilder fb = FluidFactory.getInstance().builder();
	private final String systemId;
	private final XdmNode pipeline;

	Pipeline(String systemId, XdmNodeURIResolver resolver, XProcConfiguration config) throws SAXException, IOException {
		this.systemId = systemId;
		this.config = config;
		this.resolver = resolver;
		this.pipeline = resolver.resolve(systemId);
	}

	Pipeline(InputStream in, String systemId, XdmNodeURIResolver resolver, XProcConfiguration config) throws SAXException, IOException {
		this.systemId = systemId;
		this.config = config;
		this.resolver = resolver;
		this.pipeline = resolver.parse(systemId, in);
	}

	public PipelineBuilder pipe() throws SAXException, IOException {
		return pipeSource(null);
	}

	public PipelineBuilder pipe(Object source, Type type, String... media)
			throws SAXException, IOException, XProcException {
		return pipeReader(asReader(source, type, media));
	}

	public PipelineBuilder pipeStream(InputStream source)
			throws SAXException, IOException, XProcException {
		return pipeSource(resolver.parse(null, source));
	}

	public PipelineBuilder pipeReader(Reader reader) throws SAXException, IOException, XProcException {
		return pipeSource(resolver.parse(null, reader));
	}

	private PipelineBuilder pipeSource(XdmNode source) throws SAXException, XProcException {
		XProcRuntime runtime = new XProcRuntime(config);
		runtime.setEntityResolver(resolver.getEntityResolver());
		runtime.setURIResolver(resolver);
		try {
			XPipeline xpipeline = runtime.use(pipeline);
			if (source != null) {
				xpipeline.writeTo("source", source);
			}
			return new PipelineBuilder(runtime, resolver, xpipeline, systemId);
		} catch (SaxonApiException e) {
			throw new SAXException(e);
		}
	}

	private Reader asReader(Object source, Type type, String... media)
			throws IOException {
		try {
			return fb.consume(source, null, type, media).asReader();
		} catch (TransformerConfigurationException e) {
			throw new XProcException(e);
		} catch (OpenRDFException e) {
			throw new XProcException(e);
		} catch (XMLStreamException e) {
			throw new XProcException(e);
		} catch (ParserConfigurationException e) {
			throw new XProcException(e);
		} catch (SAXException e) {
			throw new XProcException(e);
		} catch (TransformerException e) {
			throw new XProcException(e);
		}
	}

}
