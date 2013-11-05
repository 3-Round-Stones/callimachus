package org.callimachusproject.xproc;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;

public class SerializeCascadingStyleSheetStep implements XProcStep {
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
			String text = extractText(doc);

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

    private String extractText(XdmNode doc) {
        StringBuilder content = new StringBuilder();
        XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            if (child.getNodeKind() == XdmNodeKind.ELEMENT || child.getNodeKind() == XdmNodeKind.TEXT) {
                content.append(child.getStringValue());
            }
        }
        return content.toString();
    }
}
