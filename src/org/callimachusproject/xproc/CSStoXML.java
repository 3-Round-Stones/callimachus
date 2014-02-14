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

import net.sf.saxon.s9api.QName;

import org.w3c.dom.css.CSSMediaRule;
import org.w3c.dom.css.CSSPageRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;

import com.xmlcalabash.util.TreeWriter;

public class CSStoXML {
	private static final String NS = "http://callimachusproject.org/xmlns/2013/cssx#";
	private static final String PREFIX = "css";
	private static final QName _styleSheet = new QName(PREFIX, NS,
			"style-sheet");
	private static final QName _pageRule = new QName(PREFIX, NS, "page");
	private static final QName _mediaRule = new QName(PREFIX, NS, "media");
	private static final QName _query = new QName("query");
	private static final QName _styleRule = new QName(PREFIX, NS, "style");
	private static final QName _selector = new QName("selector");
	private static final QName _property = new QName(PREFIX, NS, "property");
	private static final QName _name = new QName("name");
	private static final QName _priority = new QName("priority");
	private static final QName _rule = new QName(PREFIX, NS, "rule");

	private final TreeWriter tree;

	public CSStoXML(TreeWriter tree) {
		this.tree = tree;
	}

	public void writeStyleSheet(CSSStyleSheet sheet) {
		tree.addStartElement(_styleSheet);
		tree.startContent();
		CSSRuleList rules = sheet.getCssRules();
		for (int i = 0, n = rules.getLength(); i < n; i++) {
			writeRule(rules.item(i), 0);
		}
		tree.addEndElement();
	}

	private void writeRule(CSSRule rule, int indent) {
		switch (rule.getType()) {
		case CSSRule.MEDIA_RULE:
			writeMediaRule((CSSMediaRule) rule, indent);
			break;
		case CSSRule.PAGE_RULE:
			writePageRule((CSSPageRule) rule, indent);
			break;
		case CSSRule.STYLE_RULE:
			writeStyleRule((CSSStyleRule) rule, indent);
			break;
		case CSSRule.CHARSET_RULE:
		case CSSRule.FONT_FACE_RULE:
		case CSSRule.IMPORT_RULE:
		case CSSRule.UNKNOWN_RULE:
		default:
			writeIgnorableRule(rule, indent);
		}
	}

	private void writeMediaRule(CSSMediaRule rule, int indent) {
		tree.addStartElement(_mediaRule);
		String medias = rule.getMedia().getMediaText();
		if (medias != null && medias.length() > 0) {
			tree.addAttribute(_query, medias);
		}
		tree.startContent();
		CSSRuleList rules = rule.getCssRules();
		for (int i = 0, n = rules.getLength(); i < n; i++) {
			writeRule(rules.item(i), indent + 1);
		}
		tree.addEndElement();
	}

	private void writePageRule(CSSPageRule rule, int indent) {
		tree.addStartElement(_pageRule);
		String selector = rule.getSelectorText();
		if (selector != null && selector.length() > 0) {
			tree.addAttribute(_selector, selector);
		}
		tree.startContent();
		writeDeclaration(rule.getStyle(), indent + 1);
		tree.addEndElement();
	}

	private void writeStyleRule(CSSStyleRule rule, int indent) {
		tree.addStartElement(_styleRule);
		String selector = rule.getSelectorText();
		if (selector != null && selector.length() > 0) {
			tree.addAttribute(_selector, selector);
		}
		tree.startContent();
		writeDeclaration(rule.getStyle(), indent + 1);
		tree.addEndElement();
	}

	private void writeDeclaration(CSSStyleDeclaration styles, int indent) {
		for (int i = 0, n = styles.getLength(); i < n; i++) {
			tree.addStartElement(_property);
			String name = styles.item(i);
			tree.addAttribute(_name, name);
			String priority = styles.getPropertyPriority(name);
			if (priority != null && priority.length() > 0) {
				tree.addAttribute(_priority, priority);
			}
			String value = styles.getPropertyValue(name);
			if (value != null && value.length() > 0) {
				// FIXME http://sourceforge.net/p/cssparser/bugs/41/
				value = value.replace(",,,", ",");
				tree.startContent();
				tree.addText(value);
			}
			tree.addEndElement();
		}
	}

	private void writeIgnorableRule(CSSRule rule, int indent) {
		tree.addStartElement(_rule);
		tree.startContent();
		tree.addText(rule.getCssText());
		tree.addEndElement();
	}

}
