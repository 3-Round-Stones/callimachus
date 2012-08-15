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

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;

import org.apache.xmlgraphics.util.WriterOutputStream;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.xml.AggressiveCachedURIResolver;
import org.callimachusproject.xml.CloseableURIResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcMessageListener;
import com.xmlcalabash.core.XProcRunnable;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.runtime.XPipeline;

public class PipelineBuilder implements XProcMessageListener {
	private final Logger logger = LoggerFactory
			.getLogger(PipelineBuilder.class);
	private final FluidBuilder fb = FluidFactory.getInstance().builder();
	private final XProcRuntime runtime;
	private final CloseableURIResolver resolver;
	private final XPipeline pipeline;
	private final Serialization serial;
	private final StringBuilder errors = new StringBuilder();

	PipelineBuilder(XProcRuntime runtime, URIResolver resolver,
			XPipeline pipeline, String systemId) {
		this.runtime = runtime;
		this.pipeline = pipeline;
		this.resolver = new CloseableURIResolver(resolver);
		runtime.setURIResolver(new AggressiveCachedURIResolver(systemId,
				this.resolver));
		runtime.setMessageListener(this);
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
			throw new XProcException(e.getMessage() + errors, e);
		} catch (XProcException e) {
			throw new XProcException(e.getMessage() + errors, e);
		} finally {
			resolver.close();
			errors.setLength(0);
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
		} catch (FluidException e) {
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
		} catch (FluidException e) {
			throw new XProcException(e);
		}

	}

	public void error(XProcRunnable step, XdmNode node, String message,
			QName code) {
		error(format(step, node, message));
	}

	public void error(Throwable exception) {
		error(format(exception));
	}

	public void warning(XProcRunnable step, XdmNode node, String message) {
		logger.warn(format(step, node, message));
	}

	public void warning(Throwable exception) {
		logger.warn(format(exception));
	}

	public void info(XProcRunnable step, XdmNode node, String message) {
		logger.info(format(step, node, message));
	}

	public void fine(XProcRunnable step, XdmNode node, String message) {
		logger.debug(format(step, node, message));
	}

	public void finer(XProcRunnable step, XdmNode node, String message) {
		logger.debug(format(step, node, message));
	}

	public void finest(XProcRunnable step, XdmNode node, String message) {
		logger.debug(format(step, node, message));
	}

	private void error(String message) {
		errors.append("\n").append(message);
		logger.error(message);
	}

	private String format(XProcRunnable step, XdmNode node, String message) {
		StringBuilder sb = new StringBuilder();
		if (node != null) {
			String systemId = node.getBaseURI().toASCIIString();
			int line = node.getLineNumber();
			if (systemId != null && !"".equals(systemId)) {
				sb.append(systemId);
			}
			if (line != -1) {
				sb.append("#line=").append(line);
			}
		}

		return sb.append(message).toString();
	}

	private String format(Throwable e) {
		String location = format(getStep(e), getSourceLocator(e), getStructuredQName(e));
		if (location == null || location.length() == 0)
			return e.getMessage();
		return e.getMessage() + " at " + location;
	}

	private StructuredQName getStructuredQName(Throwable exception) {
		if (exception == null)
			return null;
		StructuredQName ret = getStructuredQName(exception.getCause());
		if (ret != null)
			return ret;

		if (exception instanceof XPathException) {
			return ((XPathException) exception).getErrorCodeQName();
		} else if (exception instanceof TransformerException) {
			TransformerException tx = (TransformerException) exception;
			if (tx.getException() instanceof XPathException) {
				return ((XPathException) tx.getException())
						.getErrorCodeQName();
			}
		} else if (exception instanceof XProcException) {
			XProcException err = (XProcException) exception;
			if (err.getErrorCode() != null) {
				QName n = err.getErrorCode();
				return new StructuredQName(n.getPrefix(), n.getNamespaceURI(),
						n.getLocalName());
			}
		}
		return null;
	}

	private SourceLocator getSourceLocator(Throwable exception) {
		SourceLocator loc = null;

		if (exception instanceof TransformerException) {
			TransformerException tx = (TransformerException) exception;

			if (tx.getLocator() != null) {
				loc = tx.getLocator();
				boolean done = false;
				while (!done && loc == null) {
					if (tx.getException() instanceof TransformerException) {
						tx = (TransformerException) tx.getException();
						loc = tx.getLocator();
					} else if (exception.getCause() instanceof TransformerException) {
						tx = (TransformerException) exception.getCause();
						loc = tx.getLocator();
					} else {
						done = true;
					}
				}
			}
		}

		if (exception instanceof XProcException) {
			XProcException err = (XProcException) exception;
			loc = err.getLocator();
		}

		return loc;
	}

	private Step getStep(Throwable exception) {
		if (exception == null)
			return null;
		Step ret = getStep(exception.getCause());
		if (ret != null)
			return ret;

		if (exception instanceof XProcException) {
			XProcException err = (XProcException) exception;
			return err.getStep();
		}
		return null;
	}

	private String format(Step step, SourceLocator loc, StructuredQName qCode) {
		StringBuilder sb = new StringBuilder();
		if (step != null) {
			sb.append(step).append(" ");
		}
		if (loc != null) {
			String systemId = loc.getSystemId();
			int line = loc.getLineNumber();
			if (systemId != null && !"".equals(systemId)) {
				sb.append(systemId);
			}
			if (line != -1) {
				sb.append("#line=").append(line);
			}
		}

		if (qCode != null) {
			sb.append(" ").append(qCode.getDisplayName());
		}

		return sb.toString();
	}
}
