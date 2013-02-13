package org.callimachusproject.management;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigTemplate {
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{%[\\p{Print}&&[^\\}]]+%\\}");
	private final String template;

	public ConfigTemplate(URL url) throws IOException {
		this.template = readString(url);
	}

	public Map<String, String> getDefaultParameters() {
		Map<String, String> parameters = new HashMap<String, String>();
		Matcher matcher = PARAMETER_PATTERN.matcher(template);
		while (matcher.find()) {
			String group = matcher.group();
			String[] split = group.substring(2, group.length() - 2).split("\\s*\\|\\s*", 2);
			parameters.put(split[0], split.length < 2 ? null : split[1]);
		}
		return parameters;
	}

	public String render(Map<String, String> parameters) {
		StringBuffer result = new StringBuffer(template.length());
		Matcher matcher = PARAMETER_PATTERN.matcher(template);
		while (matcher.find()) {
			String group = matcher.group();
			String[] split = group.substring(2, group.length() - 2).split("\\s*\\|\\s*", 2);
			String value = null;
			if (parameters != null) {
				value = parameters.get(split[0]);
			}
			if (value == null && split.length < 2)
				return null;
			if (value == null) {
				value = split[1];
			}
			matcher.appendReplacement(result, value);
		}
		matcher.appendTail(result);
		return result.toString();
	}

	public Map<String, String> getParameters(URL config) throws IOException {
		List<String> names = new ArrayList<String>();
		names.add("entire");
		StringBuffer result = new StringBuffer(template.length());
		result.append("^\\s*");
		Matcher matcher = PARAMETER_PATTERN.matcher(template);
		int pos = 0;
		while (matcher.find()) {
			String group = matcher.group();
			String[] split = group.substring(2, group.length() - 2).split("\\s*\\|\\s*", 2);
			names.add(split[0]);
			result.append(Pattern.quote(template.substring(pos, matcher.start())));
			result.append("(.*)");
			pos = matcher.end();
		}
		result.append(Pattern.quote(template.substring(pos, template.length())));
		result.append("\\s*$");
		Pattern pattern = Pattern.compile(result.toString());
		Matcher m = pattern.matcher(readString(config));
		if (!m.matches())
			return null;
		Map<String, String> parameters = new LinkedHashMap<String,String>();
		for (int g=1,n=m.groupCount(); g<=n; g++) {
			parameters.put(names.get(g), m.group(g));
		}
		return parameters;
	}

	private String readString(URL url) throws IOException,
			UnsupportedEncodingException {
		StringWriter writer = new StringWriter();
		URLConnection con = url.openConnection();
		con.setRequestProperty("Accept", "text/turtle,text/plain");
		con.setRequestProperty("Accept-Charset", "UTF-8");
		InputStream in = con.getInputStream();
		InputStreamReader reader = new InputStreamReader(in, "UTF-8");
		try {
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
			}
		} finally {
			reader.close();
		}
		return writer.toString();
	}
}
