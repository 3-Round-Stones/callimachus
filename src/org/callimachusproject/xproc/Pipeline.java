package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.apache.http.client.HttpClient;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.xml.CloseableEntityResolver;
import org.callimachusproject.xml.CloseableURIResolver;
import org.callimachusproject.xml.DocumentFactory;
import org.callimachusproject.xml.XdmNodeFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.Base64;

public class Pipeline {
	private static final String XPROC_STEP = XProcConstants.c_data.getNamespaceURI();
	private static final String DATA = XProcConstants.c_data.getLocalName();
	private static final int bufSize = 912 * 8; // A multiple of 3, 4, and 75 for base64 line breaking

	private final HttpClient client;
	private final XProcConfiguration config;
	private final XdmNodeFactory resolver;
	private final String systemId;
	private final XdmNode pipeline;

	Pipeline(String systemId, HttpClient client) {
		assert systemId != null;
		this.systemId = systemId;
		this.client = client;
		this.config = null;
		this.resolver = null;
		this.pipeline = null;
	}

	Pipeline(InputStream in, String systemId, HttpClient client) throws SAXException, IOException {
		this.systemId = systemId;
		this.client = client;
		this.config = new XProcConfiguration("he", false);
		this.resolver = new XdmNodeFactory(config.getProcessor(), client);
		loadConfig(resolver, config);
		this.pipeline = resolver.parse(systemId, in);
	}

	Pipeline(Reader in, String systemId, HttpClient client) throws SAXException, IOException {
		this.systemId = systemId;
		this.client = client;
		this.config = new XProcConfiguration("he", false);
		this.resolver = new XdmNodeFactory(config.getProcessor(), client);
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
			resolver = new XdmNodeFactory(config.getProcessor(), client);
			loadConfig(resolver, config);
		}
		return pipeSource(null, resolver, config);
	}

	public Pipe pipeStreamOf(InputStream source, String systemId, String media)
			throws SAXException, IOException, XProcException, ParserConfigurationException {
		XProcConfiguration config = this.config;
		XdmNodeFactory resolver = this.resolver;
		if (config == null) {
			config = new XProcConfiguration("he", false);
			resolver = new XdmNodeFactory(config.getProcessor(), client);
			loadConfig(resolver, config);
		}
		XdmNode xml = parse(systemId, source, media, config);
		return pipeSource(xml, resolver, config);
	}

	public Pipe pipeStream(InputStream source, String systemId)
			throws SAXException, IOException, XProcException {
		XProcConfiguration config = this.config;
		XdmNodeFactory resolver = this.resolver;
		if (config == null) {
			config = new XProcConfiguration("he", false);
			resolver = new XdmNodeFactory(config.getProcessor(), client);
			loadConfig(resolver, config);
		}
		XdmNode xml = resolver.parse(systemId, source);
		return pipeSource(xml, resolver, config);
	}

	public Pipe pipeReader(Reader reader, String systemId) throws SAXException, IOException, XProcException {
		XProcConfiguration config = this.config;
		XdmNodeFactory resolver = this.resolver;
		if (config == null) {
			config = new XProcConfiguration("he", false);
			resolver = new XdmNodeFactory(config.getProcessor(), client);
			loadConfig(resolver, config);
		}
		XdmNode xml = resolver.parse(systemId, reader);
		return pipeSource(xml, resolver, config);
	}

	private Pipe pipeSource(XdmNode source, XdmNodeFactory resolver,
			XProcConfiguration config) throws SAXException, IOException {
		Pipe pipe = null;
		XProcRuntime runtime = new XProcRuntime(config);
		try {
			CloseableURIResolver uriResolver = new CloseableURIResolver(resolver);
			CloseableEntityResolver entityResolver = new CloseableEntityResolver(resolver);
			runtime.setURIResolver(uriResolver);
			runtime.setEntityResolver(entityResolver);
			runtime.getResolver().setUnderlyingModuleURIResolver(resolver);
			runtime.setHttpClient(client);
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
			return pipe = new Pipe(runtime, uriResolver, entityResolver, xpipeline, systemId);
		} catch (SaxonApiException e) {
			throw new SAXException(e);
		} finally {
			if (pipe == null) {
				runtime.close();
			}
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

	private XdmNode parse(String systemId, InputStream source, String media, XProcConfiguration config)
			throws IOException, SAXException, ParserConfigurationException {
		if (source == null && media == null)
			return null;
		try {
			FluidType type = new FluidType(InputStream.class, media);
			if (type.isXML())
				return resolver.parse(systemId, source);
			Document doc = DocumentFactory.newInstance().newDocument();
			if (systemId != null) {
				doc.setDocumentURI(systemId);
			}
			Element data = doc.createElementNS(XPROC_STEP, DATA);
			data.setAttribute("content-type", media);
			if (type.isText()) {
				Charset charset = type.getCharset();
				if (charset == null) {
					charset = Charset.forName("UTF-8");
				}
				if (source != null) {
					appendText(new InputStreamReader(source, charset), doc, data);
				}
			} else if (source != null) {
				data.setAttribute("encoding", "base64");
	            appendBase64(source, doc, data);
			}
	
			doc.appendChild(data);
	        return config.getProcessor().newDocumentBuilder().wrap(doc);
		} finally {
			if (source != null) {
				source.close();
			}
		}
	}

	private void appendText(InputStreamReader reader, Document doc, Element data)
			throws IOException {
		char buf[] = new char[bufSize];
		int len = reader.read(buf, 0, bufSize);
		while (len >= 0) {
		    data.appendChild(doc.createTextNode(new String(buf,0,len)));
		    len = reader.read(buf, 0, bufSize);
		}
	}

	private void appendBase64(InputStream source, Document doc, Element data)
			throws IOException {
		byte bytes[] = new byte[bufSize];
		int pos = 0;
		int readLen = bufSize;
		int len = source.read(bytes, 0, bufSize);
		while (len >= 0) {
		    pos += len;
		    readLen -= len;
		    if (readLen == 0) {
		    	data.appendChild(doc.createTextNode(Base64.encodeBytes(bytes)));
		        pos = 0;
		        readLen = bufSize;
		    }

		    len = source.read(bytes, pos, readLen);
		}

		if (pos > 0) {
		    byte lastBytes[] = new byte[pos];
		    System.arraycopy(bytes, 0, lastBytes, 0, pos);
		    data.appendChild(doc.createTextNode(Base64.encodeBytes(lastBytes)));
		}
	}

}
