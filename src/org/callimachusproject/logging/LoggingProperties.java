package org.callimachusproject.logging;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.callimachusproject.util.SystemProperties;

public class LoggingProperties {
	private static final Pattern KEY_VALUE_REGEX = Pattern
			.compile("\\s*(.*)\\s*=\\s*(.*)\\s*$");
	private static final LoggingProperties system = new LoggingProperties(SystemProperties.getLoggingPropertiesFile());

	public static LoggingProperties getInstance() {
		return system;
	}

	private final File file;

	public LoggingProperties(File file) {
		this.file = file;
	}

	public synchronized Map<String, String> getLoggingProperties()
			throws IOException {
		if (file == null || !file.isFile())
			return Collections.emptyMap();
		Map<String, String> properties = getAllLoggingProperties();
		Iterator<String> iter = properties.keySet().iterator();
		while (iter.hasNext()) {
			if (iter.next().contains("password")) {
				iter.remove();
			}
		}
		return properties;
	}

	public synchronized void setLoggingProperties(Map<String, String> lines)
			throws IOException, MessagingException {
		if (file != null && file.canRead()) {
			Map<String, String> map = getAllLoggingProperties();
			map.putAll(lines);
			lines = map;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PrintWriter writer = new PrintWriter(out);
			try {
				for (Map.Entry<String, String> line : lines.entrySet()) {
					if (line.getValue() == null) {
						writer.println(line.getKey());
					} else {
						writer.println(line.getKey() + "=" + line.getValue());
					}
				}
			} finally {
				writer.close();
			}
		} finally {
			out.close();
		}
		byte[] data = out.toByteArray();
		LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(data));
		if (file != null) {
			file.getParentFile().mkdirs();
			FileOutputStream stream = new FileOutputStream(file);
			try {
				stream.write(data);
			} finally {
				stream.close();
			}
		}
	}

	private Map<String, String> getAllLoggingProperties()
			throws FileNotFoundException, IOException {
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		Map<String, String> lines = new LinkedHashMap<String, String>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			Matcher m = KEY_VALUE_REGEX.matcher(line);
			if (m.matches()) {
				lines.put(m.group(1), m.group(2));
			} else {
				lines.put(line, null);
			}
		}
		bufferedReader.close();
		return lines;
	}
}
