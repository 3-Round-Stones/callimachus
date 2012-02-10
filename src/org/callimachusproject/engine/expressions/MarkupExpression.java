package org.callimachusproject.engine.expressions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.openrdf.model.Value;

public class MarkupExpression implements Expression {
	/** ^\{(\?[a-zA-Z]\w*)\} */
	private static final Pattern VARIABLE = Pattern
			.compile("^\\{(\\?[a-zA-Z]\\w*)\\}");
	/** ^\{"(([^"\n]|\\")*?)"\} */
	private static final Pattern STRING1 = Pattern
			.compile("^\\{\"(([^\"\\n]|\\\\\")*?)\"\\}");
	/** ^\{'(([^'\n]|\\')*?)'\} */
	private static final Pattern STRING2 = Pattern
			.compile("^\\{'(([^'\\n]|\\\\')*?)'\\}");
	private static final String NCNameChar = "a-zA-Z0-9\\-\\._"
			+ "\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD"
			+ "\\u00B7\\u0300-\\u036F\\u203F-\\u2040";
	/** ^\{([NCNameChar]*:[NCNameChar]*?)\} */
	private static final Pattern PROPERTY = Pattern.compile("^\\{(["
			+ NCNameChar + "]*:[" + NCNameChar + "]*?)\\}");

	private final List<Expression> exprs;
	private final Expression expression;

	public MarkupExpression(String text, Map<String, String> namespaces,
			AbsoluteTermFactory tf) throws RDFParseException {
		if (text.indexOf('{') < 0) {
			expression = new TextExpression(text);
			exprs = Collections.singletonList(expression);
		} else {
			exprs = new ArrayList<Expression>();
			parse(text, namespaces, tf);
			if (exprs.size() == 1) {
				expression = exprs.get(0);
			} else {
				expression = null;
			}
		}
	}

	@Override
	public String bind(Map<String, Value> variables) {
		if (expression != null)
			return expression.bind(variables);
		StringBuilder sb = new StringBuilder();
		for (Expression exp : exprs) {
			sb.append(exp.bind(variables));
		}
		return sb.toString();
	}

	@Override
	public String getTemplate() {
		if (expression != null)
			return expression.getTemplate();
		StringBuilder sb = new StringBuilder();
		for (Expression exp : exprs) {
			sb.append(exp.getTemplate());
		}
		return sb.toString();
	}

	@Override
	public List<RDFEvent> pattern(Node subject) {
		if (expression != null)
			return expression.pattern(subject);
		List<RDFEvent> list = new ArrayList<RDFEvent>();
		for (Expression exp : exprs) {
			list.addAll(exp.pattern(subject));
		}
		return list;
	}

	private void parse(String text, Map<String, String> namespaces,
			AbsoluteTermFactory tf) throws RDFParseException {
		if (text.length() < 1)
			return;
		Matcher m;
		int open = text.indexOf('{');
		if (open < 0 || open == text.length() - 1) {
			exprs.add(new TextExpression(text));
		} else if (open != 0) {
			exprs.add(new TextExpression(text.substring(0, open)));
			parse(text.substring(open), namespaces, tf);
		} else if ((m = VARIABLE.matcher(text)).find()) {
			exprs.add(new VariableExpression(m.group(1)));
		} else if ((m = STRING1.matcher(text)).find()) {
			exprs.add(new QuotedString(m.group(1)));
		} else if ((m = STRING2.matcher(text)).find()) {
			exprs.add(new QuotedString(m.group(1)));
		} else if ((m = PROPERTY.matcher(text)).find()) {
			exprs.add(new PropertyExpression(m.group(1), namespaces, tf));
		} else {
			int next = text.indexOf('{', open + 1);
			if (next < 0) {
				exprs.add(new TextExpression(text));
			} else {
				exprs.add(new TextExpression(text.substring(0, next)));
				parse(text.substring(next), namespaces, tf);
			}
		}
	}

}
