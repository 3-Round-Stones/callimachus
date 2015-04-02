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
package org.callimachusproject.rewrite;

import static org.callimachusproject.util.PercentCodec.encode;

import java.lang.reflect.Array;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URITemplate {
	private static final Pattern EXPRESSIONS = Pattern
			.compile("\\{[\\+#\\./;\\?\\&]?(?:[A-Za-z0-9_%\\.:\\*]+,?)+\\}");
	private static final Pattern VARIABLE = Pattern
			.compile("([A-Za-z0-9_%][A-Za-z0-9_%\\.]*):?([0-9]*)(\\*)?");

	private static enum Expansion {
		/* {var} */
		SIMPLE(null, null, ',', true),
		/* {+var} */
		RESERVED('+', null, ',', false),
		/* {#var} */
		FRAGMENT('#', '#', ',', false),
		/* {.var} */
		DOT('.', '.', '.', true),
		/* {/var} */
		SEGMENT('/', '/', '/', true),
		/* {;var} */
		PATH(';', true, ';', true),
		/* {?var} */
		QUERY('?', false, '&', true),
		/* {&var} */
		CONTIN('&', false, '&', true);
		final Character operator;
		final String prefix;
		final boolean named;
		final boolean empty;
		final char separator;
		final boolean encode;

		Expansion(Character operator, Character prefix, char separator,
				boolean encode) {
			this.operator = operator;
			this.prefix = prefix == null ? "" : prefix.toString();
			this.named = false;
			this.empty = false;
			this.separator = separator;
			this.encode = encode;
		}

		Expansion(Character operator, boolean empty, char separator,
				boolean encode) {
			this.operator = operator;
			this.prefix = operator.toString();
			this.named = true;
			this.empty = empty;
			this.separator = separator;
			this.encode = encode;
		}
	}

	private final String template;
	private final Set<String> variables = new LinkedHashSet<String>();

	public URITemplate(String template) {
		this.template = template;
		Matcher m = EXPRESSIONS.matcher(template);
		while (m.find()) {
			String expr = m.group();
			Matcher v = VARIABLE.matcher(expr);
			while (v.find()) {
				variables.add(v.group(1));
			}
		}
	}

	public String toString() {
		return template;
	}

	public boolean containsVariableName(String name) {
		return variables.contains(name);
	}

	public CharSequence process(Map<String, ?> variables) {
		if (template.indexOf('{') < 0)
			return template;
		StringBuilder sb = new StringBuilder(255);
		for (int i = 0, n = template.length(); i < n; i++) {
			String expr = getExpression(template, i);
			if (expr != null) {
				Expansion ex = getExpansion(expr);
				CharSequence value = values(expr, variables, ex);
				if (value != null) {
					sb.append(ex.prefix).append(value);
				}
				i += expr.length() - 1;
			} else {
				sb.append(template.charAt(i));
			}
		}
		return sb;
	}

	private String getExpression(String template, int i) {
		char chr = template.charAt(i);
		if (chr == '{' && i + 2 < template.length()) {
			int j = template.indexOf('}', i);
			if (j > i) {
				String expr = template.substring(i, j + 1);
				if (EXPRESSIONS.matcher(expr).matches())
					return expr;
			}
		}
		return null;
	}

	private Expansion getExpansion(String expr) {
		Character prefix = expr.charAt(1);
		for (Expansion ex : Expansion.values()) {
			if (prefix.equals(ex.operator)) {
				return ex;
			}
		}
		return Expansion.SIMPLE;
	}

	private CharSequence values(String expression, Map<String, ?> variables,
			Expansion ex) {
		StringBuilder sb = null;
		Matcher m = VARIABLE.matcher(expression);
		while (m.find()) {
			String name = m.group(1);
			String max = m.group(2);
			int maxLength = max.length() == 0 ? Integer.MAX_VALUE : Integer
					.parseInt(max);
			boolean explode = "*".equals(m.group(3));
			CharSequence value = value(name, variables, maxLength, explode, ex);
			if (value != null) {
				if (sb == null) {
					sb = new StringBuilder();
				} else {
					sb.append(ex.separator);
				}
				if (ex.encode) {
					sb.append(value);
				} else {
					sb.append(value);
				}
			}
		}
		return sb;
	}

	private CharSequence value(String name, Map<String, ?> variables,
			int maxLength, boolean explode, Expansion ex) {
		Object o = variables.get(name);
		if (o == null) {
			return null;
		} else if (o instanceof CharSequence) {
			return inline(name, (CharSequence) o, maxLength, ex);
		} else if (o.getClass().isArray() && Array.getLength(o) == 0) {
			return null;
		} else if (o.getClass().isArray()) {
			StringBuilder sb = null;
			for (int i = 0; i < Array.getLength(o); i++) {
				String value = Array.get(o, i).toString();
				if (value != null) {
					if (sb == null) {
						sb = new StringBuilder();
						sb.append(inline(name, value, maxLength, ex));
					} else if (explode) {
						sb.append(ex.separator);
						sb.append(inline(name, value, maxLength, ex));
					} else {
						sb.append(',');
						sb.append(inline(null, value, maxLength, ex));
					}
				}
			}
			return sb;
		} else if (o instanceof Map) {
			StringBuilder sb = null;
			Map<?, ?> map = (Map<?, ?>) o;
			for (Map.Entry<?, ?> e : map.entrySet()) {
				String key = e.getKey().toString();
				String value = e.getValue().toString();
				if (explode) {
					if (sb == null) {
						sb = new StringBuilder();
					} else {
						sb.append(ex.separator);
					}
					sb.append(inline(null, key, maxLength, ex));
					sb.append('=');
					sb.append(inline(null, value, maxLength, ex));
				} else {
					if (sb == null) {
						sb = new StringBuilder();
						sb.append(inline(name, key, maxLength, ex));
					} else {
						sb.append(',');
						sb.append(inline(null, key, maxLength, ex));
					}
					sb.append(',');
					sb.append(inline(null, value, maxLength, ex));
				}
			}
			return sb;
		} else {
			return inline(name, o.toString(), maxLength, ex);
		}
	}

	private CharSequence inline(String name, CharSequence value, int maxLength,
			Expansion ex) {
		if (value.length() > maxLength) {
			value = value.subSequence(0, maxLength);
		}
		if (name != null && ex.named) {
			if (ex.empty && value.length() == 0) {
				return name;
			} else if (ex.encode) {
				return name + '=' + encode(value.toString());
			} else {
				return name + '=' + value;
			}
		} else if (ex.encode) {
			return encode(value.toString());
		} else {
			return value;
		}
	}

}
