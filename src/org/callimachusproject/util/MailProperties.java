package org.callimachusproject.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

public class MailProperties {
	private static final Pattern KEY_VALUE_REGEX = Pattern
			.compile("\\s*(.*?)\\s*=\\s*(.*?)\\s*$");
	private static final MailProperties system = new MailProperties(SystemProperties.getMailPropertiesFile());

	public static MailProperties getInstance() {
		return system;
	}

	private final File file;

	public MailProperties(File file) {
		this.file = file;
	}

	public synchronized Properties loadMailProperties() throws IOException {
		Properties properties = new Properties();
		if (file != null && file.isFile()) {
			try {
				InputStream stream = new FileInputStream(file);
				try {
					properties.load(stream);
				} finally {
					stream.close();
				}
			} catch (FileNotFoundException e) {
				// skip
			}
		}
		return properties;
	}

	public synchronized Map<String, String> getMailProperties()
			throws IOException {
		if ( file == null || !file.isFile())
			return Collections.emptyMap();
		Map<String, String> properties = getAllMailProperties();
		Iterator<String> iter = properties.keySet().iterator();
		while (iter.hasNext()) {
			if (iter.next().contains("password")) {
				iter.remove();
			}
		}
		return properties;
	}

	public synchronized void setMailProperties(Map<String, String> lines)
			throws IOException, MessagingException {
		if (file == null)
			throw new IllegalStateException(
					"The system property java.mail.properties must be provided on VM start");
		if (file.canRead()) {
			Map<String, String> map = getAllMailProperties();
			map.putAll(lines);
			lines = map;
		}
		validate(lines);
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
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
	}

	private Map<String, String> getAllMailProperties()
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

	private void validate(Map<String, String> lines) throws MessagingException {
		Map<String, String> map = new LinkedHashMap<String, String>(lines);
		Iterator<String> iter = map.values().iterator();
		while (iter.hasNext()) {
			if (iter.next() == null) {
				iter.remove();
			}
		}
		Properties properties = new Properties();
		properties.putAll(map);
		if (properties.containsKey("mail.transport.protocol")) {
			Session session = Session.getInstance(properties);
			String user = session.getProperty("mail.user");
			String password = session.getProperty("mail.password");
			Transport tr = session.getTransport();
			try {
				tr.connect(user, password);
			} finally {
				tr.close();
			}
		}
	}
}
