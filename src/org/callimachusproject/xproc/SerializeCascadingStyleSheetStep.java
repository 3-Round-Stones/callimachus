package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleSheet;

import com.steadystate.css.parser.CSSOMParser;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;

public class SerializeCascadingStyleSheetStep implements XProcStep {
	private static final String NS = "http://callimachusproject.org/xmlns/2013/cssx#";
	private static final String PREFIX = "css";
	private static final QName _styleSheet = new QName(PREFIX, NS,
			"style-sheet");
	private static final QName _content_type = new QName("content-type");

	private final XProcRuntime runtime;
	private final XAtomicStep step;
	private String contentType = "text/css";
	private ReadablePipe source;
	private WritablePipe result;

	public SerializeCascadingStyleSheetStep(XProcRuntime runtime, XAtomicStep step) {
		this.runtime = runtime;
		this.step = step;
	}

	@Override
	public void setParameter(QName name, RuntimeValue value) {
		throw new XProcException("No parameters allowed.");
	}

	@Override
	public void setParameter(String port, QName name, RuntimeValue value) {
		setParameter(name, value);
	}

	@Override
	public void setOption(QName name, RuntimeValue value) {
		switch (name.getLocalName()) {
		case "content-type":
			contentType = value.getString();
			break;
		}
	}

	public void setInput(String port, ReadablePipe pipe) {
		source = pipe;
	}

	public void setOutput(String port, WritablePipe pipe) {
		result = pipe;
	}

	public void reset() {
		source.resetReader();
		result.resetWriter();
	}

	@Override
	public void run() throws SaxonApiException {
		while (source.moreDocuments()) {
			XdmNode doc = source.read();
			XdmNode root = S9apiUtils.getDocumentElement(doc);
			String contentType = getContentType(root);
			String text = toCSS(doc, contentType);

			TreeWriter tree = new TreeWriter(runtime);
			tree.startDocument(step.getNode().getBaseURI());
			tree.addStartElement(XProcConstants.c_data);
			tree.addAttribute(_content_type, contentType);
			tree.startContent();
			tree.addText(text);
			tree.addEndElement();
			tree.endDocument();
			result.write(tree.getResult());
		}
	}

	private String getContentType(XdmNode root) {
		if (this.contentType == null) {
			return root.getAttributeValue(_content_type);
		} else {
			return this.contentType;
		}
	}

	private String toCSS(XdmNode doc, String contentType) {
		StringWriter writer = new StringWriter();
		new XMLtoCSS(writer).writeDocument(doc);
		String text = writer.toString();
		// verify
		if (isStyleSheet(doc)) {
			parse(text, doc.getBaseURI().toASCIIString(), contentType, null);
		}
		return text;
	}

	private boolean isStyleSheet(XdmNode doc) {
		if (_styleSheet.equals(doc.getNodeName()))
			return true;
        for (XdmNode node : new RelevantNodes(doc, Axis.CHILD,true)) {
            if (_styleSheet.equals(node.getNodeName())) {
                return true;
            }
        }
        return false;
	}

	private CSSStyleSheet parse(String text, String baseURI, String contentType,
			String charset) {
		CSSOMParser parser = new CSSOMParser();
		InputSource css = new InputSource(new StringReader(text));
		css.setURI(baseURI);
		if (contentType != null) {
			css.setMedia(contentType);
		}
		if (charset != null) {
			css.setEncoding(charset);
		}
		parser.setErrorHandler(new XProcErrorHandler(step.getNode()));
		try {
			return parser.parseStyleSheet(css, null, css.getURI());
		} catch (IOException e) {
			throw XProcException.dynamicError(30, step.getNode(), e, e.getMessage());
		}
	}
}
