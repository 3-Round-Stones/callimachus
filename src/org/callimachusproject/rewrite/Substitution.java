package org.callimachusproject.rewrite;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Substitution {

	private static final Pattern NAMED_GROUP_PATTERN = Pattern
			.compile("\\(\\?<(\\w+)>");

	private final String regex;
	private final Pattern pattern;
	private final String substitution;
	private final List<String> groupNames;

	public static Substitution compile(String regex, String substitution, String flags) {
		return new Substitution(regex, substitution, flags);
	}

	public static Substitution compile(String regex, String substitution) {
		return new Substitution(regex, substitution, "");
	}

	public static Substitution compile(String command) {
		int end = command.indexOf('\n');
		if (end < 0) {
			end = command.length();
		}
		int split = command.lastIndexOf(' ', end);
		if (split >= 0) {
			String pattern = command.substring(0, split);
			String replacement = command.substring(split + 1);
			return Substitution.compile(pattern, replacement);
		} else {
			return Substitution.compile(".*", command);
		}
	}

	private Substitution(String regex, String substitution, String flags) {
		this.regex = regex;
		this.substitution = substitution;
		this.groupNames = extractGroupNames(regex);
		this.pattern = Pattern.compile(buildStandardPattern(regex, groupNames), flags(flags));
	}

	public String toString() {
		return regex + " " + substitution;
	}

	public String pattern() {
		return regex;
	}

	public String substitution() {
		return substitution;
	}

	public int flags() {
		return pattern.flags();
	}

	public boolean containsVariableName(String name) {
		return substitution.contains("{" + name + "}");
	}

	public String replace(CharSequence input) {
		Map<String, String> parameters = Collections.emptyMap();
		return replace(input, parameters);
	}

	public String replace(CharSequence input, Map<String, ?> variables) {
		int dollar = substitution.indexOf('$');
		int percent = substitution.indexOf('{');
		if (dollar < 0 && percent < 0)
			return substitution;
		Matcher m = pattern.matcher(input);
		StringBuilder sb = new StringBuilder(255);
		int position = 0;
		while (m.find() && position < input.length()) {
			sb.append(input, position, m.start());
			appendSubstitution(m, variables, sb);
			position = m.end();
		}
		if (position == 0)
			return null;
		sb.append(input, position, input.length());
		return sb.toString();
	}

	private void appendSubstitution(Matcher m,
			Map<String, ?> variables, StringBuilder sb) {
		boolean encode = false;
		for (int i = 0, n = substitution.length(); i < n; i++) {
			char chr = substitution.charAt(i);
			if (chr == '=') {
				encode = true;
				sb.append(chr);
			} else if (chr == '?' || chr == '&') {
				encode = false;
				sb.append(chr);
			} else if (chr == '\\' && i + 1 < n) {
				sb.append(substitution.charAt(++i));
			} else if (chr == '$' && i + 1 < n) {
				char next = substitution.charAt(++i);
				if (next == '$') {
					sb.append(next);
				} else if (next >= '0' && next <= '9' && m != null) {
					int idx = next - '0';
					appendGroup(m, idx, encode, sb);
				} else {
					sb.append(chr);
					--i;
				}
			} else if (chr == '{' && i + 2 < n) {
				int j = substitution.indexOf('}', i);
				if (j > i) {
					String name = substitution.substring(i + 1, j);
					if (name.startsWith("{")) {
						sb.append(name);
					} else {
						appendVariable(name, m, variables, encode, sb);
					}
					i = j;
				} else {
					sb.append(chr);
				}
			} else {
				sb.append(chr);
			}
		}
	}

	private void appendGroup(Matcher m, int idx, boolean encode, StringBuilder sb) {
		if (idx <= m.groupCount()) {
			sb.append(inline(m.group(idx), encode));
		}
	}

	private void appendVariable(String name, Matcher m, Map<String, ?> variables,
			boolean encode, StringBuilder sb) {
		String value;
		int g = groupNames.indexOf(name) + 1;
		if (g > 0) {
			value = m.group(g);
		} else {
			Object o = variables.get(name);
			if (o == null) {
				value = null;
			} else if (o instanceof String) {
				value = (String) o;
			} else if (o.getClass().isArray() && Array.getLength(o) == 0) {
				value = null;
			} else if (o.getClass().isArray()) {
				value = Array.get(o, 0).toString();
			} else {
				value = o.toString();
			}
		}
		if (value != null) {
			sb.append(inline(value, encode));
		}
	}

	private CharSequence inline(String value, boolean encode) {
		if (encode) {
			try {
				return URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new AssertionError(e);
			}
		} else {
			return value;
		}
	}

	private List<String> extractGroupNames(String namedPattern) {
		List<String> groupNames = new ArrayList<String>();
		Matcher matcher = NAMED_GROUP_PATTERN.matcher(namedPattern);
		while (matcher.find()) {
			groupNames.add(matcher.group(1));
		}
		return groupNames;
	}

	private String buildStandardPattern(String namedPattern,
			List<String> groupNames) {
		String regex = NAMED_GROUP_PATTERN.matcher(namedPattern)
				.replaceAll("(");
		for (int g = 1, n = groupNames.size(); g <= n; g++) {
			regex = regex.replace("\\k<" + groupNames.get(g - 1) + ">", "\\" + g);
		}
		return regex;
	}

	private int flags(String flags) {
		int f = 0;
		for (char c : flags.toCharArray()) {
			switch (c) {
				case 's':
					f |= Pattern.DOTALL;
					break;
				case 'm':
					f |= Pattern.MULTILINE;
					break;
				case 'i':
					f |= Pattern.CASE_INSENSITIVE;
					break;
				case 'x':
					f |= Pattern.COMMENTS;
					break;
				case 'd':
					f |= Pattern.UNIX_LINES;
					break;
				case 'u':
					f |= Pattern.UNICODE_CASE;
					break;
			}
		}
		return f;
	}

}