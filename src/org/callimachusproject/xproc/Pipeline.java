package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Enumeration;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.xml.CloseableEntityResolver;
import org.callimachusproject.xml.CloseableURIResolver;
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

	Pipeline(String systemId) {
		assert systemId != null;
		this.systemId = systemId;
		this.config = null;
		this.resolver = null;
		this.pipeline = null;
	}

	Pipeline(InputStream in, String systemId) throws SAXException, IOException {
		this.systemId = systemId;
		this.config = new XProcConfiguration("he", false);
		this.resolver = new XdmNodeFactory(config.getProcessor());
		loadConfig(resolver, config);
		this.pipeline = resolver.parse(systemId, in);
	}

	Pipeline(Reader in, String systemId) throws SAXException, IOException {
		this.systemId = systemId;
		this.config = new XProcConfiguration("he", false);
		this.resolver = new XdmNodeFactory(config.getProcessor());
		loadConfig(resolver, config);
		this.pipeline = resolver.parse(systemId, in);
	}

	@Override
	public String toString() {
		if (systemId != null)
			return systemId;
		return super.toString();
	}

	public String getSystemId() {
		return systemId;
	}

	public Pipe pipe() throws SAXException, IOException {
		XProcConfiguration config = this.config;
		XdmNodeFactory resolver = this.resolver;
		if (config == null) {
			config = new XProcConfiguration("he", false);
			resolver = new XdmNodeFactory(config.getProcessor());
			loadConfig(resolver, config);
		}
		return pipeSource(null, resolver, config);
	}

	public Pipe pipe(Object source, String systemId, Type type, String... media)
			throws SAXException, IOException, XProcException {
		return pipeReader(asReader(source, systemId, type, media), systemId);
	}

	public Pipe pipeStream(InputStream source, String systemId)
			throws SAXException, IOException, XProcException {
		XProcConfiguration config = this.config;
		XdmNodeFactory resolver = this.resolver;
		if (config == null) {
			config = new XProcConfiguration("he", false);
			resolver = new XdmNodeFactory(config.getProcessor());
			loadConfig(resolver, config);
		}
		return pipeSource(resolver.parse(systemId, source), resolver, config);
	}

	public Pipe pipeReader(Reader reader, String systemId) throws SAXException, IOException, XProcException {
		XProcConfiguration config = this.config;
		XdmNodeFactory resolver = this.resolver;
		if (config == null) {
			config = new XProcConfiguration("he", false);
			resolver = new XdmNodeFactory(config.getProcessor());
			loadConfig(resolver, config);
		}
		return pipeSource(resolver.parse(systemId, reader), resolver, config);
	}

	private Pipe pipeSource(XdmNode source, XdmNodeFactory resolver, XProcConfiguration config) throws SAXException, XProcException, IOException {
		XProcRuntime runtime = new XProcRuntime(config);
		try {
			CloseableURIResolver uriResolver = new CloseableURIResolver(resolver);
			CloseableEntityResolver entityResolver = new CloseableEntityResolver(resolver);
			runtime.setURIResolver(uriResolver);
			runtime.setEntityResolver(entityResolver);
			XdmNode doc = this.pipeline;
			if (doc == null) {
				doc = resolver.parse(systemId);
			}
			if (doc == null)
				throw new InternalServerError("Missing pipeline: " + systemId);
			XPipeline xpipeline = runtime.use(doc);
			if (source != null) {
				xpipeline.writeTo("source", source);
			}
			return new Pipe(runtime, uriResolver, entityResolver, xpipeline, systemId);
		} catch (SaxonApiException e) {
			throw new SAXException(e);
		} finally {
			runtime.close();
		}
	}

	private void loadConfig(XdmNodeFactory resolver, XProcConfiguration config) throws IOException {
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

	private Reader asReader(Object source, String systemId, Type type, String... media)
			throws IOException {
		try {
			return fb.consume(source, systemId, type, media).asReader();
		} catch (FluidException e) {
			throw new XProcException(e);
		}
	}

}
