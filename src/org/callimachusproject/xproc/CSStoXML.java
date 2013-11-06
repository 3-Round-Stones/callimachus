package org.callimachusproject.xproc;

import net.sf.saxon.s9api.QName;

import org.w3c.dom.css.CSSMediaRule;
import org.w3c.dom.css.CSSPageRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.stylesheets.MediaList;

import com.xmlcalabash.util.TreeWriter;

public class CSStoXML {
	private static final String NS = "http://callimachusproject.org/xmlns/2013/cssx#";
	private static final String PREFIX = "css";
	private static final QName _styleSheet = new QName(PREFIX, NS,
			"style-sheet");
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
		tree.startContent();
		MediaList medias = rule.getMedia();
		for (int i = 0, n = medias.getLength(); i < n; i++) {
			tree.addStartElement(_media);
			tree.startContent();
			tree.addText(medias.item(i));
			tree.addEndElement();
		}

		CSSRuleList rules = rule.getCssRules();
		for (int i = 0, n = rules.getLength(); i < n; i++) {
			writeRule(rules.item(i), indent + 1);
		}
		tree.addEndElement();
	}

	private void writePageRule(CSSPageRule rule, int indent) {
		tree.addStartElement(_pageRule);
		tree.startContent();
		String selector = rule.getSelectorText();
		if (selector != null && selector.length() > 0) {
			tree.addStartElement(_selector);
			tree.startContent();
			tree.addText(selector);
			tree.addEndElement();
		}
		writeDeclaration(rule.getStyle(), indent + 1);
		tree.addEndElement();
	}

	private void writeStyleRule(CSSStyleRule rule, int indent) {
		tree.addStartElement(_styleRule);
		tree.startContent();
		String selector = rule.getSelectorText();
		if (selector != null && selector.length() > 0) {
			tree.addStartElement(_selector);
			tree.startContent();
			tree.addText(selector);
			tree.addEndElement();
		}
		writeDeclaration(rule.getStyle(), indent + 1);
		tree.addEndElement();
	}

	private void writeDeclaration(CSSStyleDeclaration styles, int indent) {
		for (int i = 0, n = styles.getLength(); i < n; i++) {
			tree.addStartElement(_style);
			tree.startContent();
			String name = styles.item(i);
			tree.addStartElement(_name);
			tree.startContent();
			tree.addText(name);
			tree.addEndElement();
			String value = styles.getPropertyValue(name);
			if (value != null && value.length() > 0) {
				tree.addStartElement(_value);
				tree.startContent();
				tree.addText(value);
				tree.addEndElement();
			}
			String priority = styles.getPropertyPriority(name);
			if (priority != null && priority.length() > 0) {
				tree.addStartElement(_priority);
				tree.startContent();
				tree.addText(priority);
				tree.addEndElement();
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
