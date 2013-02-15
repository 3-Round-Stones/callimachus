package org.callimachusproject.setup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.callimachusproject.util.SystemProperties;

public class CallimachusConf {
	private static final Pattern WSPACE = Pattern.compile("\\s");

	private final File file;
	private final File defaultFile;

	public CallimachusConf(	File file) {
		this(file, SystemProperties.getConfigDefaultsFile());
	}

	public CallimachusConf(File file, File defaultFile) {
		this.file = file;
		this.defaultFile = defaultFile;
	}

	public String toString() {
		return file.toString();
	}

	public synchronized String[] getCallimachusWebappOrigins() throws IOException {
		String webapps = getProperty("PRIMARY_ORIGIN");
		if (webapps != null && webapps.trim().length() > 0)
			return webapps.trim().split("\\s+");
		String origins = getProperty("ORIGIN");
		if (origins != null && origins.trim().length() > 0)
			return origins.trim().split("\\s+");
		return new String[0];
	}

	public synchronized String getProperty(String key) throws IOException {
		if (!file.isFile() && !defaultFile.isFile())
			return null;
		Pattern prefix = Pattern.compile("^\\s*" + Pattern.quote(key)
				+ "\\s*=\\s*\\\"?([^\\\"]*)\\\"?\\s*$");
		String value = getProperty(prefix, file);
		if (value == null)
			return getProperty(prefix, defaultFile);
		return value;
	}

	public synchronized void setProperty(String key, String value)
			throws IOException {
		Pattern prefix = Pattern.compile("^\\s*#?\\s*" + Pattern.quote(key)
				+ "\\s*=\\s*\\\"?([^\\\"]*)\\\"?\\s*$");
		List<String> lines = getServerConfiguration();
		file.getParentFile().mkdirs();
		boolean replaced = false;
		PrintWriter writer = new PrintWriter(file);
		try {
			for (String line : lines) {
				if (prefix.matcher(line).matches()) {
					replaced = true;
					if (value != null && WSPACE.matcher(value).find()) {
						writer.println(key + "=\"" + value.replace("\"", "")
								+ "\"");
					} else if (value != null) {
						writer.println(key + "=" + value);
					} else if (line.startsWith("#")) {
						writer.println(line);
					} else {
						writer.println("#" + line);
					}
				} else {
					writer.println(line);
				}
			}
			if (!replaced && value != null) {
				writer.println(key + "=" + value);
			}
		} finally {
			writer.close();
		}
	}

	private String getProperty(Pattern pattern, File file)
			throws FileNotFoundException, IOException {
		if (!file.isFile())
			return null;
		BufferedReader rd = new BufferedReader(new FileReader(file));
		try {
			String line;
			while ((line = rd.readLine()) != null) {
				Matcher m = pattern.matcher(line);
				if (m.find()) {
					return m.group(1);
				}
			}
		} finally {
			rd.close();
		}
		return null;
	}

	private List<String> getServerConfiguration() throws IOException {
		List<String> lines = getServerConfiguration(file);
		if (lines.isEmpty())
			return getServerConfiguration(defaultFile);
		return lines;
	}

	private List<String> getServerConfiguration(File file)
			throws FileNotFoundException, IOException {
		if (!file.isFile())
			return Collections.emptyList();
		List<String> lines = new ArrayList<String>();
		FileReader fileReader = new FileReader(file);
		BufferedReader reader = new BufferedReader(fileReader);
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		} finally {
			reader.close();
		}
		return lines;
	}
}
