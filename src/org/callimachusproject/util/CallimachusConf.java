package org.callimachusproject.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;


public class CallimachusConf {
	private static final String DEFAULT_REPOSITORY_ID;
	static {
		File repositoryConfig = SystemProperties.getRepositoryConfigFile();
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));
		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		try {
			InputStream in = repositoryConfig.toURI().toURL().openStream();
			InputStreamReader reader = new InputStreamReader(in);
			try {
				rdfParser.parse(reader, base);
			} finally {
				reader.close();
			}
			Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
					RepositoryConfigSchema.REPOSITORY);
			RepositoryConfig config = RepositoryConfig.create(graph, node);
			DEFAULT_REPOSITORY_ID = config.getID();
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		} catch (IOException e) {
			throw new AssertionError(e);
		} catch (OpenRDFException e) {
			throw new AssertionError(e);
		}
	}

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

	public synchronized String[] getWebappOrigins() throws IOException {
		Map<String, String> map = getOriginRepositoryIDs();
		if (map == null || map.isEmpty())
			return new String[0];
		return map.keySet().toArray(new String[map.size()]);
	}

	public Map<String, String> getOriginRepositoryIDs() throws IOException {
		String value = getProperty("ORIGIN");
		if (value == null || value.trim().length() == 0)
			return Collections.emptyMap();
		String[] items = value.trim().split("\\s+");
		Map<String, String> map = new LinkedHashMap<String, String>(items.length);
		for (String item : items) {
			int idx = item.indexOf('#');
			if (idx < 0) {
				map.put(item, DEFAULT_REPOSITORY_ID);
			} else {
				map.put(item.substring(0, idx), item.substring(idx + 1));
			}
		}
		return map;
	}

	public void setOriginRepositoryIDs(Map<String, String> map) throws IOException {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, String>> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, String> e = iter.next();
			if (e.getKey() == null || e.getKey().length() == 0 || !e.getKey().contains("://"))
				throw new IllegalArgumentException("Invalid origin: " + e.getKey());
			if (e.getValue() == null || e.getValue().length() == 0)
				throw new IllegalArgumentException("Invalid repositoryID: " + e.getValue());
			sb.append(e.getKey()).append('#').append(e.getValue());
			if (iter.hasNext()) {
				sb.append(' ');
			}
		}
		setProperty("ORIGIN", sb.toString());
	}

	public String getAppVersion() throws IOException {
		return getProperty("APP_VER");
	}

	public void setAppVersion(String version) throws IOException {
		setProperty("APP_VER", version);
	}

	public String getServerName() throws IOException {
		return getProperty("SERVER_NAME");
	}

	public void setServerName(String name) throws IOException {
		setProperty("SERVER_NAME", name);
	}

	public int[] getPorts() throws IOException {
		String portStr = getProperty("PORT");
		int[] ports = new int[0];
		if (portStr != null && portStr.trim().length() > 0) {
			String[] values = portStr.trim().split("\\s+");
			ports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				ports[i] = Integer.parseInt(values[i]);
			}
		}
		return ports;
	}

	public void setPorts(int[] ports) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<ports.length; i++) {
			sb.append(ports[i]);
			if (i <ports.length - 1) {
				sb.append(' ');
			}
		}
		setProperty("PORT", sb.toString());
	}

	public int[] getSslPorts() throws IOException {
		String sslportStr = getProperty("SSLPORT");
		int[] sslports = new int[0];
		if (sslportStr != null && sslportStr.trim().length() > 0) {
			String[] values = sslportStr.trim().split("\\s+");
			sslports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				sslports[i] = Integer.parseInt(values[i]);
			}
		}
		return sslports;
	}

	public void setSslPorts(int[] ports) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<ports.length; i++) {
			sb.append(ports[i]);
			if (i <ports.length - 1) {
				sb.append(' ');
			}
		}
		setProperty("SSLPORT", sb.toString());
	}

	private synchronized String getProperty(String key) throws IOException {
		if (!file.isFile() && !defaultFile.isFile())
			return null;
		Pattern prefix = Pattern.compile("^\\s*" + Pattern.quote(key)
				+ "\\s*=\\s*\\\"?([^\\\"]*)\\\"?\\s*$");
		String value = getProperty(prefix, file);
		if (value == null)
			return getProperty(prefix, defaultFile);
		return value;
	}

	private synchronized void setProperty(String key, String value)
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
