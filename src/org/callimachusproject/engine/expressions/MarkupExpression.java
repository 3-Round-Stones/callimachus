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
package org.callimachusproject.engine.expressions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.Location;

import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.TermOrigin;

public class MarkupExpression implements Expression {
	/** ^\{(\?[a-zA-Z]\w*)\} */
	private static final Pattern VARIABLE = Pattern
			.compile("^\\{(\\?[a-zA-Z]\\w*)\\}");
	/** ^\{"(([^"\n]|\\")*?)"\} */
	private static final Pattern STRING1 = Pattern
			.compile("^\\{(\"([^\"\\n]|\\\\\")*?\")\\}");
	/** ^\{'(([^'\n]|\\')*?)'\} */
	private static final Pattern STRING2 = Pattern
			.compile("^\\{('([^'\\n]|\\\\')*?')\\}");
	private static final String NCNameChar = "a-zA-Z0-9\\-\\._"
			+ "\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD"
			+ "\\u00B7\\u0300-\\u036F\\u203F-\\u2040";
	/** ^\{([NCNameChar]*:[NCNameChar]*?)\} */
	private static final Pattern PROPERTY = Pattern.compile("^\\{(["
			+ NCNameChar + "]*:[" + NCNameChar + "]*?)\\}");
	private static final String URIChar = "a-zA-Z0-9\\-\\._~%!\\$\\&'\\(\\)\\*\\+,;=:/\\?\\#\\[\\]@";
	private static final String PathChar = "\\<\\>\\^\\/\\|\\*\\+\\?\\!\\(\\)";
	private static final Pattern PATH = Pattern.compile("^\\{([" + NCNameChar
			+ URIChar + "]*[" + PathChar + "][" + NCNameChar + URIChar
			+ PathChar + "]*)\\}");

	private final List<Expression> exprs;
	private final Expression expression;

	public MarkupExpression(CharSequence text, NamespaceContext namespaces,
			Location location, AbsoluteTermFactory tf) throws RDFParseException {
		if (indexOf(text, '{') < 0) {
			expression = new TextExpression(text, location);
			exprs = Collections.singletonList(expression);
		} else {
			exprs = new ArrayList<Expression>();
			parse(text, namespaces, location, tf);
			if (exprs.size() == 1) {
				expression = exprs.get(0);
			} else {
				expression = null;
			}
		}
	}

	@Override
	public Location getLocation() {
		if (expression != null)
			return expression.getLocation();
		for (Expression exp : exprs) {
			return exp.getLocation();
		}
		return null;
	}

	@Override
	public String toString() {
		if (expression != null)
			return expression.toString();
		StringBuilder sb = new StringBuilder();
		for (Expression exp : exprs) {
			sb.append(exp.toString());
		}
		return sb.toString();
	}

	@Override
	public CharSequence bind(ExpressionResult variables) {
		if (expression != null)
			return expression.bind(variables);
		StringBuilder sb = new StringBuilder();
		for (Expression exp : exprs) {
			sb.append(exp.bind(variables));
		}
		return sb;
	}

	@Override
	public CharSequence getTemplate() {
		if (expression != null)
			return expression.getTemplate();
		StringBuilder sb = new StringBuilder();
		for (Expression exp : exprs) {
			sb.append(exp.getTemplate());
		}
		return sb;
	}

	@Override
	public boolean isPatternPresent() {
		if (expression != null)
			return expression.isPatternPresent();
		for (Expression exp : exprs) {
			if (exp.isPatternPresent())
				return true;
		}
		return false;
	}

	@Override
	public List<RDFEvent> pattern(Node subject, TermOrigin origin, Location location) {
		if (expression != null)
			return expression.pattern(subject, origin, location);
		List<RDFEvent> list = new ArrayList<RDFEvent>();
		for (Expression exp : exprs) {
			list.addAll(exp.pattern(subject, origin, location));
		}
		return list;
	}

	private void parse(CharSequence text, NamespaceContext namespaces,
			Location loc, AbsoluteTermFactory tf) throws RDFParseException {
		int p = 0;
		while (p < text.length()) {
			Matcher m;
			char chr = text.charAt(p);
			switch (chr) {
			case '{':
				if (p != 0) {
					exprs.add(new TextExpression(text.subSequence(0, p), loc));
					text = text.subSequence(p, text.length());
					p = 0;
					continue;
				} else if ((m = VARIABLE.matcher(text)).find()) {
					exprs.add(new VariableExpression(m.group(1), loc));
					text = text.subSequence(m.end(), text.length());
					p = 0;
					continue;
				} else if ((m = STRING1.matcher(text)).find()) {
					exprs.add(new QuotedString(m.group(1), loc));
					text = text.subSequence(m.end(), text.length());
					p = 0;
					continue;
				} else if ((m = STRING2.matcher(text)).find()) {
					exprs.add(new QuotedString(m.group(1), loc));
					text = text.subSequence(m.end(), text.length());
					p = 0;
					continue;
				} else if ((m = PROPERTY.matcher(text)).find()) {
					exprs.add(new PropertyExpression(m.group(1), namespaces,
							loc, tf));
					text = text.subSequence(m.end(), text.length());
					p = 0;
					continue;
				} else if ((m = PATH.matcher(text)).find()) {
					exprs.add(new PathExpression(m.group(1), namespaces,
							loc, tf));
					text = text.subSequence(m.end(), text.length());
					p = 0;
					continue;
				}
			default:
				p++;
			}
		}
		if (text.length() > 0) {
			exprs.add(new TextExpression(text, loc));
		}
	}

	private int indexOf(CharSequence text, int ch) {
        int max = text.length();
        for (int i = 0; i < max ; i++) {
            if (text.charAt(i) == ch)
                return i;
        }
        return -1;
    }

}
