package org.callimachusproject.xproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
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

	PipelineBuilder(XProcRuntime runtime, URIResolver resolver, XPipeline pipeline, String systemId) {
		this.runtime = runtime;
		this.pipeline = pipeline;
		this.resolver = new CloseableURIResolver(resolver);
		runtime.setURIResolver(new AggressiveCachedURIResolver(systemId, this.resolver));
	}

	public void passOption(String key, String value) {
		pipeline.passOption(new QName(key), new RuntimeValue(value));
	}

	public void streamTo(OutputStream out) throws XProcException, IOException {
		try {
			pipeline.run();
			Serialization serial = pipeline.getSerialization("result");
			if (serial == null) {
				// The node's a hack
				serial = new Serialization(runtime, pipeline.getNode());
			}
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

	public InputStream asStream() throws XProcException, IOException  {
		return (InputStream) as(InputStream.class);
	}

	public Object as(Type type, String... media) throws XProcException, IOException {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(8192);
		streamTo(outStream);
		try {
			return fb.consume(outStream, null, ByteArrayOutputStream.class).as(
					type, media);
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
