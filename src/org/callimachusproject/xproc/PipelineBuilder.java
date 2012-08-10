package org.callimachusproject.xproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import org.apache.xmlgraphics.util.WriterOutputStream;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.xml.AggressiveCachedURIResolver;
import org.callimachusproject.xml.CloseableURIResolver;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.runtime.XPipeline;

public class PipelineBuilder {
	private final FluidBuilder fb = FluidFactory.getInstance().builder();
	private final XProcRuntime runtime;
	private final CloseableURIResolver resolver;
	private final XPipeline pipeline;
	private final Serialization serial;

	PipelineBuilder(XProcRuntime runtime, URIResolver resolver,
			XPipeline pipeline, String systemId) {
		this.runtime = runtime;
		this.pipeline = pipeline;
		this.resolver = new CloseableURIResolver(resolver);
		runtime.setURIResolver(new AggressiveCachedURIResolver(systemId,
				this.resolver));
		Serialization serialization = pipeline.getSerialization("result");
		if (serialization == null) {
			this.serial = new Serialization(runtime, null);
		} else {
			this.serial = serialization;
		}
		String encoding = serial.getEncoding();
		if (encoding == null) {
			serial.setEncoding(encoding = Charset.defaultCharset().name());
		}
		String mediaType = serial.getMediaType();
		if (mediaType == null) {
			serial.setMediaType(mediaType = "application/xml");
		} else if (mediaType.startsWith("text/")
				&& !mediaType.contains("charset=")) {
			serial.setMediaType(mediaType + ";charset=" + encoding);
		}
	}

	public void passOption(String key, String value) {
		pipeline.passOption(new QName(key), new RuntimeValue(value));
	}

	public String getMediaType() {
		return serial.getMediaType();
	}

	public void setMediaType(String mediaType) {
		serial.setMediaType(mediaType);
	}

	public String getEncoding() {
		return serial.getEncoding();
	}

	public void setEncoding(String encoding) {
		serial.setEncoding(encoding);
	}

	public void streamTo(OutputStream out) throws XProcException, IOException {
		try {
			pipeline.run();
			WritableDocument wd = new WritableDocument(runtime, null, serial,
					out);
			ReadablePipe rpipe = pipeline.readFrom("result");
			while (rpipe.moreDocuments()) {
				wd.write(rpipe.read());
			}
		} catch (SaxonApiException e) {
			throw new XProcException(e);
		} finally {
			resolver.close();
		}
	}

	public void writeTo(Writer out) throws XProcException, IOException {
		streamTo(new WriterOutputStream(out, getEncoding()));
	}

	public InputStream asStream() throws XProcException, IOException {
		return (InputStream) as(InputStream.class);
	}

	public Object as(Type type, String... media) throws XProcException,
			IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
		streamTo(out);
		try {
			ReadableByteChannel ch = ChannelUtil.newChannel(out.toByteArray());
			return fb.channel(ch, null, getMediaType()).as(type, media);
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

	public String asString() throws XProcException, IOException {
		StringWriter out = new StringWriter(8192);
		writeTo(out);
		try {
			CharSequence cs = out.getBuffer();
			Fluid fluid = fb.consume(cs, null, CharSequence.class, "text/xml");
			return fluid.asString("text/xml");
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
