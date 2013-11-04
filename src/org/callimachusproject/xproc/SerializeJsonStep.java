package org.callimachusproject.xproc;

import java.io.PrintWriter;
import java.io.StringWriter;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
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
import com.xmlcalabash.util.XMLtoJSON;

public class SerializeJsonStep implements XProcStep {
	private static final QName _content_type = new QName("content-type");

	private final XProcRuntime runtime;
	private final XAtomicStep step;
	private String contentType = "application/json";
	private ReadablePipe source;
	private WritablePipe result;

	public SerializeJsonStep(XProcRuntime runtime, XAtomicStep step) {
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
			XdmNode json = getJsonNode(root);

			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.print(XMLtoJSON.convert(json));
			writer.close();

			TreeWriter tree = new TreeWriter(runtime);
			tree.startDocument(step.getNode().getBaseURI());
			tree.addStartElement(XProcConstants.c_data);
			tree.addAttribute(_content_type, contentType);
			tree.startContent();
			tree.addText(out.toString());
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

	private XdmNode getJsonNode(XdmNode root) {
		QName name = root.getNodeName();
		if (XProcConstants.c_data.equals(name)
				|| XProcConstants.c_body.equals(name)) {
			XdmNode jchild = null;
			XdmSequenceIterator iter = root.axisIterator(Axis.CHILD);
			while (iter.hasNext()) {
				XdmItem item = iter.next();
				if (item instanceof XdmNode) {
					XdmNode child = (XdmNode) item;
					if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
						if (jchild != null) {
							throw new XProcException(
									"Found more than one JSON element?");
						} else {
							jchild = child;
						}
					}
				}
			}
			return jchild;
		} else {
			return root;
		}
	}
}
