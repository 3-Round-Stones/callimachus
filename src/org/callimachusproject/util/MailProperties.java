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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailProperties implements MailPropertiesMXBean {
	private static final MailProperties system = new MailProperties(
			System.getProperty("java.mail.properties"));

	public static MailProperties getInstance() {
		return system;
	}

	private final File file;

	public MailProperties(String file) {
		this(new File(file));
	}

	public MailProperties(File file) {
		this.file = file;
	}

	public synchronized String[] getMailProperties() throws IOException {
		if (!file.isFile())
			return new String[0];
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		List<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			lines.add(line);
		}
		bufferedReader.close();
		return lines.toArray(new String[lines.size()]);
	}

	public synchronized void setMailProperties(String[] lines)
			throws IOException {
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		try {
			PrintWriter writer = new PrintWriter(out);
			try {
				for (String line : lines) {
					writer.println(line);
				}
			} finally {
				writer.close();
			}
		} finally {
			out.close();
		}
	}

	public synchronized Properties loadMailProperties() throws IOException {
		Properties properties = new Properties();
		if (file.isFile()) {
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
}
