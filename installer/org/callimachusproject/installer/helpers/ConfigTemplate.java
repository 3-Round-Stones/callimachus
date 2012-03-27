package org.callimachusproject.installer.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigTemplate {
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{%[\\p{Print}&&[^\\}]]+%\\}");
	private final String template;

	public ConfigTemplate(URL url) throws IOException {
		StringWriter writer = new StringWriter();
		URLConnection con = url.openConnection();
		con.setRequestProperty("Accept", "text/turtle");
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
		this.template = writer.toString();
	}

	public Map<String, String> getDefaultParameters() {
		Map<String, String> parameters = new HashMap<String, String>();
		Matcher matcher = PARAMETER_PATTERN.matcher(template);
		while (matcher.find()) {
			String group = matcher.group();
			String[] split = group.substring(2, group.length() - 2).split("\\s*\\|\\s*", 2);
			parameters.put(split[0], split[1]);
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
			if (value == null) {
				value = split[1];
			}
			matcher.appendReplacement(result, value);
		}
		matcher.appendTail(result);
		return result.toString();
	}
}
