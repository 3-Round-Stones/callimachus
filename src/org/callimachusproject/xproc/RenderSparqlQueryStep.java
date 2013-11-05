package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.xml.DocumentFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;

public class RenderSparqlQueryStep implements XProcStep {
	private static final QName _content_type = new QName("content-type");
	public static final QName _encoding = new QName("", "encoding");
	private static final FluidFactory FF = FluidFactory.getInstance();
	private static final DocumentFactory df = DocumentFactory.newInstance();
	private final TemplateEngine engine;
	private final FluidBuilder fb = FF.builder();
	private final Map<String, String> parameters = new LinkedHashMap<String, String>();
	private final XProcRuntime runtime;
	private final XAtomicStep step;
	private ReadablePipe sourcePipe = null;
	private ReadablePipe templatePipe = null;
	private WritablePipe resultPipe = null;
	private String outputBase;

	public RenderSparqlQueryStep(XProcRuntime runtime, XAtomicStep step) {
		this.runtime = runtime;
		this.step = step;
		URI docId = step.getNode().getDocumentURI();
		assert docId != null;
		engine = TemplateEngine.newInstance(runtime.getHttpClient());
	}

	@Override
	public void setParameter(QName name, RuntimeValue value) {
		parameters.put(name.getLocalName(), value.getString());
	}

	@Override
	public void setParameter(String port, QName name, RuntimeValue value) {
		setParameter(name, value);
	}

	@Override
	public void setOption(QName name, RuntimeValue value) {
		if ("output-base-uri".equals(name.getClarkName())) {
			outputBase = value.getString();
		}
	}

	public void setInput(String port, ReadablePipe pipe) {
		if ("source".equals(port)) {
			sourcePipe = pipe;
		} else {
			templatePipe = pipe;
		}
	}

	public void setOutput(String port, WritablePipe pipe) {
		if ("result".equals(port)) {
			resultPipe = pipe;
		} else {
			throw new XProcException("No other outputs allowed.");
		}
	}

	public void reset() {
		sourcePipe.resetReader();
		templatePipe.resetReader();
		resultPipe.resetWriter();
	}

	public void run() throws SaxonApiException {
		if (templatePipe == null || !templatePipe.moreDocuments()) {
			throw XProcException.dynamicError(6, step.getNode(),
					"No template provided.");
		}
		try {
			XdmNode template = templatePipe.read();
			XdmNode xml = render(template, sourcePipe);
			if (resultPipe != null && xml != null) {
				if (outputBase != null && outputBase.length() > 0) {
					xml.getUnderlyingNode().setSystemId(resolve(outputBase));
				}
				resultPipe.write(xml);
			}
		} catch (SaxonApiException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (FluidException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (IOException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (TemplateException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		} catch (ParserConfigurationException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		}
	}

	private XdmNode render(XdmNode template, ReadablePipe sourcePipe) throws SaxonApiException,
			IOException, FluidException, TemplateException,
			ParserConfigurationException {
		if (sourcePipe != null && sourcePipe.moreDocuments()) {
			XdmNode query = sourcePipe.read();
			String queryBaseURI = query.getBaseURI().toASCIIString();
			String queryString = getQueryString(query);
			return render(template, queryString, queryBaseURI);
		} else {
			return render(template, template.getBaseURI().toASCIIString());
		}
	}

	private String getQueryString(XdmNode document) {
		XdmNode root = S9apiUtils.getDocumentElement(document);

		if ((XProcConstants.c_data.equals(root.getNodeName()) && "application/octet-stream"
				.equals(root.getAttributeValue(_content_type)))
				|| "base64".equals(root.getAttributeValue(_encoding))) {
			byte[] decoded = Base64.decode(root.getStringValue());
			return new String(decoded);
		}
		return root.getStringValue();
	}

	private XdmNode render(XdmNode t, String queryString, String queryBaseURI)
			throws SaxonApiException, IOException, FluidException,
			TemplateException, ParserConfigurationException {
		String tempId = t.getBaseURI().toASCIIString();
		Reader template = asReader(t);
		Template tem = engine.getTemplate(template, tempId);
		Document doc = toDocument(tem.getQueryString(queryString));

		DocumentBuilder xdmBuilder = newDocumentBuilder();
		return xdmBuilder.build(new DOMSource(doc, queryBaseURI));
	}

	private XdmNode render(XdmNode t, String baseURI)
			throws SaxonApiException, IOException, FluidException,
			TemplateException, ParserConfigurationException {
		String tempId = t.getBaseURI().toASCIIString();
		Reader template = asReader(t);
		Template tem = engine.getTemplate(template, tempId);
		Document doc = toDocument(tem.getQueryString());

		DocumentBuilder xdmBuilder = newDocumentBuilder();
		return xdmBuilder.build(new DOMSource(doc, baseURI));
	}

	private Document toDocument(String result)
			throws ParserConfigurationException {
		Document doc = df.newDocument();
		Element data = doc.createElementNS("http://www.w3.org/ns/xproc-step",
				"data");
		data.setAttribute("content-type", "application/sparql-query");
		data.appendChild(doc.createTextNode(result));
		doc.appendChild(data);
		return doc;
	}

	private Reader asReader(XdmNode document) throws SaxonApiException,
			IOException, FluidException {
		String sysId = document.getBaseURI().toASCIIString();
		StringWriter sw = new StringWriter();
		Serializer serializer = new Serializer();
		serializer.setOutputWriter(sw);
		S9apiUtils.serialize(runtime, document, serializer);
		return fb
				.consume(sw.getBuffer(), sysId, CharSequence.class, "text/xml")
				.asReader();
	}

	private String resolve(String href) {
		String base = step.getNode().getBaseURI().toASCIIString();
		if (href == null)
			return base;
		return TermFactory.newInstance(base).resolve(href);
	}

	private DocumentBuilder newDocumentBuilder() {
		return runtime.getConfiguration().getProcessor().newDocumentBuilder();
	}

}
