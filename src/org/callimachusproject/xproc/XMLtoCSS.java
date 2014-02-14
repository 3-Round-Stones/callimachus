/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.xproc;

import java.io.PrintWriter;
import java.io.Writer;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.util.RelevantNodes;

public class XMLtoCSS {
	private static final String INDENT = "\t";
	private static final String NS = "http://callimachusproject.org/xmlns/2013/cssx#";
	private static final String PREFIX = "css";
	private static final QName _pageRule = new QName(PREFIX, NS, "page");
	private static final QName _mediaRule = new QName(PREFIX, NS, "media");
	private static final QName _query = new QName("query");
	private static final QName _styleRule = new QName(PREFIX, NS, "style");
	private static final QName _selector = new QName("selector");
	private static final QName _property = new QName(PREFIX, NS, "property");
	private static final QName _name = new QName("name");
	private static final QName _priority = new QName("priority");
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
		if (sheet == null)
			return;
		QName name = sheet.getNodeName();
		if (_mediaRule.equals(name)) {
			writeMediaRule(sheet, indent);
		} else if (_pageRule.equals(name)) {
			writePageRule(sheet, indent);
		} else if (_styleRule.equals(name)) {
			writeStyleRule(sheet, indent);
		} else if (_rule.equals(name)) {
			writeIgnorableRule(sheet, indent);
		} else if (_property.equals(name)) {
			writeProperty(sheet, indent);
		} else {
			for (XdmNode child : new RelevantNodes(sheet, Axis.CHILD, true)) {
				writeStyleSheet(child, 0);
			}
		}
	}

	private void writeMediaRule(XdmNode rule, int indent) {
		writer.write(indent(indent));
		writer.write("@media ");
		String media = rule.getAttributeValue(_query);
		if (media != null && media.length() > 0) {
			writer.write(media);
			writer.write(" ");
		}
		writer.write("{\n");
		for (XdmNode node : new RelevantNodes(rule, Axis.CHILD, true)) {
			writeStyleSheet(node, indent + 1);
		}
		writer.write(indent(indent));
		writer.write("}\n");
	}

	private void writePageRule(XdmNode rule, int indent) {
		writer.write(indent(indent));
		writer.write("@page ");
		String selector = rule.getAttributeValue(_selector);
		if (selector != null && selector.length() > 0) {
			writer.write(selector);
			writer.write(" ");
		}
		writer.write("{\n");
		for (XdmNode property : new RelevantNodes(rule, _property)) {
			if (_property.equals(property.getNodeName())) {
				writeProperty(property, indent + 1);
			}
		}
		writer.write(indent(indent));
		writer.write("}\n");
	}

	private void writeStyleRule(XdmNode rule, int indent) {
		writer.write(indent(indent));
		String selector = rule.getAttributeValue(_selector);
		if (selector != null && selector.length() > 0) {
			writer.write(selector);
			writer.write(" ");
		}
		writer.write("{\n");
		for (XdmNode property : new RelevantNodes(rule, _property)) {
			if (_property.equals(property.getNodeName())) {
				writeProperty(property, indent + 1);
			}
		}
		writer.write(indent(indent));
		writer.write("}\n");
	}

	private void writeProperty(XdmNode property, int indent) {
		writer.write(indent(indent));
		writer.write(property.getAttributeValue(_name));
		writer.write(":");
		String value = property.getStringValue().trim();
		if (value != null && value.length() > 0) {
			writer.write(" ");
			writer.write(value);
		}
		String priority = property.getAttributeValue(_priority);
		if (priority != null && priority.length() > 0) {
			writer.write(" !");
			writer.write(priority);
		}
		writer.write(";");
		if (indent > 0) {
			writer.write("\n");
		}
	}

	private void writeIgnorableRule(XdmNode rule, int indent) {
		writer.write(indent(indent));
		writer.write(rule.getStringValue().trim());
		writer.write("\n");
	}

	private String indent(int indent) {
		StringBuilder sb = new StringBuilder(INDENT.length() * indent);
		for (int i = 0; i < indent; i++) {
			sb.append(INDENT);
		}
		return sb.toString();
	}

}
