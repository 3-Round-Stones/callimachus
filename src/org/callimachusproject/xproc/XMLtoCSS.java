package org.callimachusproject.xproc;

import java.io.PrintWriter;
import java.io.Writer;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import com.xmlcalabash.util.RelevantNodes;

public class XMLtoCSS {
	private static final String INDENT = "\t";
	private static final String NS = "http://callimachusproject.org/xmlns/2013/cssx#";
	private static final String PREFIX = "css";
	private static final QName _pageRule = new QName(PREFIX, NS, "page");
	private static final QName _mediaRule = new QName(PREFIX, NS, "media-rule");
	private static final QName _media = new QName(PREFIX, NS, "media");
	private static final QName _styleRule = new QName(PREFIX, NS, "style-rule");
	private static final QName _selector = new QName(PREFIX, NS, "selector");
	private static final QName _style = new QName(PREFIX, NS, "style");
	private static final QName _name = new QName(PREFIX, NS, "name");
	private static final QName _value = new QName(PREFIX, NS, "value");
	private static final QName _priority = new QName(PREFIX, NS, "priority");
	private static final QName _rule = new QName(PREFIX, NS, "rule");

	private final PrintWriter writer;

	public XMLtoCSS(Writer writer) {
		this(new PrintWriter(writer));
	}

	public XMLtoCSS(PrintWriter writer) {
		this.writer = writer;
	}

	public void writeDocument(XdmNode doc) {
		writeStyleSheet(doc, 0);
		writer.flush();
	}

	private void writeStyleSheet(XdmNode sheet, int indent) {
		QName name = sheet.getNodeName();
		if (_mediaRule.equals(name)) {
			writeMediaRule(sheet, indent);
		} else if (_pageRule.equals(name)) {
			writePageRule(sheet, indent);
		} else if (_styleRule.equals(name)) {
			writeStyleRule(sheet, indent);
		} else if (_rule.equals(name)) {
			writeIgnorableRule(sheet, indent);
		} else if (_style.equals(name)) {
			writeStyle(sheet, indent);
		} else {
			for (XdmNode child : new RelevantNodes(sheet, Axis.CHILD, true)) {
				writeStyleSheet(child, 0);
			}
		}
	}

	private void writeMediaRule(XdmNode rule, int indent) {
		writer.write(indent(indent));
		writer.write("@media ");
		int m = 0;
		for (XdmNode media : new RelevantNodes(rule, _media)) {
			if (_media.equals(media.getNodeName())) {
				if (0 < m++) {
					writer.write(", ");
				}
				writer.write(extractText(media));
			}
		}

		writer.write(" {\n");
		for (XdmNode node : new RelevantNodes(rule, Axis.CHILD, true)) {
			writeStyleSheet(node, indent + 1);
		}
		writer.write(indent(indent));
		writer.write("}\n");
	}

	private void writePageRule(XdmNode rule, int indent) {
		writer.write(indent(indent));
		writer.write("@page ");
		for (XdmNode node : new RelevantNodes(rule, _selector)) {
			if (_selector.equals(node.getNodeName())) {
				String selector = extractText(node);
				if (selector != null && selector.length() > 0) {
					writer.write(selector);
					writer.write(" ");
				}
			}
		}
		writer.write("{\n");
		for (XdmNode style : new RelevantNodes(rule, _style)) {
			writeStyle(style, indent + 1);
		}
		writer.write(indent(indent));
		writer.write("}\n");
	}

	private void writeStyleRule(XdmNode rule, int indent) {
		writer.write(indent(indent));
		for (XdmNode node : new RelevantNodes(rule, _selector)) {
			if (_selector.equals(node.getNodeName())) {
				String selector = extractText(node);
				if (selector != null && selector.length() > 0) {
					writer.write(selector);
					writer.write(" ");
				}
			}
		}
		writer.write("{\n");
		for (XdmNode style : new RelevantNodes(rule, _style)) {
			writeStyle(style, indent + 1);
		}
		writer.write(indent(indent));
		writer.write("}\n");
	}

	private void writeStyle(XdmNode style, int indent) {
		for (XdmNode nameNode : new RelevantNodes(style, _name)) {
			if (_name.equals(nameNode.getNodeName())) {
				String name = extractText(nameNode);
				writer.write(indent(indent));
				writer.write(name);
				writer.write(":");
				for (XdmNode valueNode : new RelevantNodes(style, _value)) {
					if (_value.equals(valueNode.getNodeName())) {
						String value = extractText(valueNode);
						if (value != null && value.length() > 0) {
							writer.write(" ");
							writer.write(value);
						}
					}
				}
				for (XdmNode priorityNode : new RelevantNodes(style, _priority)) {
					if (_priority.equals(priorityNode.getNodeName())) {
						String priority = extractText(priorityNode);
						if (priority != null && priority.length() > 0) {
							writer.write(" !");
							writer.write(priority);
						}
					}
				}
				writer.write(";\n");
			}
		}
	}

	private void writeIgnorableRule(XdmNode rule, int indent) {
		writer.write(indent(indent));
		writer.write(extractText(rule));
		writer.write("\n");
	}

	private String indent(int indent) {
		StringBuilder sb = new StringBuilder(INDENT.length() * indent);
		for (int i = 0; i < indent; i++) {
			sb.append(INDENT);
		}
		return sb.toString();
	}

	private String extractText(XdmNode doc) {
		StringBuilder content = new StringBuilder();
		XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
		while (iter.hasNext()) {
			XdmNode child = (XdmNode) iter.next();
			if (child.getNodeKind() == XdmNodeKind.ELEMENT
					|| child.getNodeKind() == XdmNodeKind.TEXT) {
				content.append(child.getStringValue());
			}
		}
		return content.toString();
	}

}
