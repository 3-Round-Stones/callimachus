package org.callimachusproject.installer;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class Configure {
	private static final String LOGGING_PROPERTIES = "logging.properties";
	private static final String MAIL_PROPERTIES = "mail.properties";
	private static final String SERVER_CONF = "callimachus.conf";
	private static final String START_SCRIPT = "callimachus-start";
	private static final String STOP_SCRIPT = "callimachus-stop";
	private final File dir;
	private SetupProxy setup;

	public Configure(File dir) {
		this.dir = dir;
	}

	public Properties getServerConfiguration() throws IOException {
		File file = new File(new File(dir, "etc"), SERVER_CONF);
		return readProperties(file, "META-INF/templates/callimachus.conf");
	}

	public void setServerConfiguration(Properties properties)
			throws IOException {
		File file = new File(new File(dir, "etc"), SERVER_CONF);
		file.getParentFile().mkdirs();
		FileWriter out = new FileWriter(file);
		try {
			BufferedWriter bw = new BufferedWriter(out);
			bw.write("#" + new Date().toString());
			bw.newLine();
			Enumeration<?> e = properties.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String val = properties.getProperty(key);
				bw.write(key + "=" + val);
				bw.newLine();
			}
			bw.flush();
		} finally {
			out.close();
		}
	}

	public Properties getMailProperties() throws IOException {
		File file = new File(new File(dir, "etc"), MAIL_PROPERTIES);
		return readProperties(file, "META-INF/templates/mail.properties");
	}

	public void setMailProperties(Properties properties) throws IOException {
		File file = new File(new File(dir, "etc"), MAIL_PROPERTIES);
		writeProperties(properties, file);
	}

	public Properties getLoggingProperties() throws IOException {
		File file = new File(new File(dir, "etc"), LOGGING_PROPERTIES);
		return readProperties(file, "META-INF/templates/logging.properties");
	}

	public void setLoggingProperties(Properties properties) throws IOException {
		File file = new File(new File(dir, "etc"), LOGGING_PROPERTIES);
		writeProperties(properties, file);
	}

	public Map<String,URL> getRepositoryConfigTemplates() throws IOException {
		Properties properties = new Properties();
		ClassLoader cl = getClass().getClassLoader();
		InputStream in = cl.getResourceAsStream("META-INF/templates/repository-index.properties");
		if (in != null) {
			try {
				properties.load(in);
			} finally {
				in.close();
			}
		}
		Map<String,URL> map = new LinkedHashMap<String, URL>();
		Enumeration<?> iter = properties.propertyNames();
		while (iter.hasMoreElements()) {
			String key = (String) iter.nextElement();
			map.put(key, cl.getResource(properties.getProperty(key)));
		}
		return map;
	}

	public boolean isServerRunning() {
		File run = new File(dir, "run");
		return run.exists() && run.list().length > 0;
	}

	public boolean stopServer() throws Exception {
		Process p = exec(STOP_SCRIPT);
		if (p == null)
			return false;
		InputStream in = p.getInputStream();
		try {
			int read;
			byte[] buf = new byte[1024];
			while ((read = in.read(buf)) >= 0) {
				System.out.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
		Thread.sleep(1000);
		return p.exitValue() == 0;
	}

	public boolean startServer() throws Exception {
		Process p = exec(START_SCRIPT);
		if (p == null)
			return false;
		InputStream in = p.getInputStream();
		try {
			int read;
			byte[] buf = new byte[1024];
			while ((read = in.read(buf)) >= 0) {
				System.out.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
		Thread.sleep(1000);
		return p.exitValue() == 0;
	}

	public boolean isWebBrowserSupported() {
		if (findInPath("sh") == null)
			return false;
		if (!Desktop.isDesktopSupported())
			return false;
        Desktop desktop = Desktop.getDesktop();
        return desktop.isSupported(Desktop.Action.BROWSE);
	}

	public void openWebBrowser(String url) throws IOException {
		java.awt.Desktop.getDesktop().browse(URI.create(url));
	}

	public List<String> getDefaultOrigins(String ports) throws SocketException {
		Collection<String> hosts = getAllCanonicalHostNames();
		Set<String> numbers = new LinkedHashSet<String>(Arrays.asList(ports
				.split("\\s+")));
		numbers.remove("");
		List<String> result = new ArrayList<String>(hosts.size()
				* numbers.size());
		for (String host : hosts) {
			for (String port : numbers) {
				if ("80".equals(port)) {
					result.add("http://" + host);
				} else if ("443".equals(port)) {
					result.add("https://" + host);
				} else {
					result.add("http://" + host + ":" + port);
				}
			}
		}
		return result;
	}

	public void connect(URL ctemplate, Map<String,String> parameters) throws ClassNotFoundException, Exception {
		File lib = new File(dir, "lib");
		setup = new SetupProxy(lib);
		setup.connect(dir, new ConfigTemplate(ctemplate).render(parameters));
	}

	public void disconnect() {
		setup.disconnect();
	}

	public void createOrigin(String origin) throws Exception {
		setup.createOrigin(origin, getCallimachusCarUrl());
	}

	public void createVirtualHost(String virtual, String origin)
			throws Exception {
		setup.createVirtualHost(virtual, origin);
	}

	public void createRealm(String realm, String origin)
			throws Exception {
		setup.createRealm(realm, origin);
	}

	private Properties readProperties(File file, String path) throws FileNotFoundException,
			IOException {
		Properties properties = new Properties();
		if (file.exists()) {
			FileInputStream in = new FileInputStream(file);
			try {
				properties.load(in);
			} finally {
				in.close();
			}
		} else {
			ClassLoader cl = getClass().getClassLoader();
			InputStream in = cl.getResourceAsStream(path);
			if (in != null) {
				try {
					properties.load(in);
				} finally {
					in.close();
				}
			}
		}
		return properties;
	}

	private void writeProperties(Properties properties, File file)
			throws FileNotFoundException, IOException {
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		try {
			properties.store(out, null);
		} finally {
			out.close();
		}
	}

	private Collection<String> getAllCanonicalHostNames() throws SocketException {
		Collection<String> set = new TreeSet<String>();
		Enumeration<NetworkInterface> ifaces = NetworkInterface
				.getNetworkInterfaces();
		while (ifaces.hasMoreElements()) {
			NetworkInterface iface = ifaces.nextElement();
			Enumeration<InetAddress> raddrs = iface.getInetAddresses();
			while (raddrs.hasMoreElements()) {
				InetAddress raddr = raddrs.nextElement();
				set.add(raddr.getCanonicalHostName());
			}
			Enumeration<NetworkInterface> virtualIfaces = iface
					.getSubInterfaces();
			while (virtualIfaces.hasMoreElements()) {
				NetworkInterface viface = virtualIfaces.nextElement();
				Enumeration<InetAddress> vaddrs = viface.getInetAddresses();
				while (vaddrs.hasMoreElements()) {
					InetAddress vaddr = vaddrs.nextElement();
					set.add(vaddr.getCanonicalHostName());
				}
			}
		}
		return set;
	}

	private URL getCallimachusCarUrl() throws IOException {
		ClassLoader cl = getClass().getClassLoader();
		InputStream in = cl.getResourceAsStream("META-INF/callimachus-webapps");
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String path = reader.readLine();
			return cl.getResource(path);
		} finally {
			in.close();
		}
	}

	private Process exec(String script) throws IOException {
		File bin = new File(dir, "bin");
		File sh = new File(bin, script + ".sh");
		File bat = new File(bin, script + ".bat");
		if (sh.exists() && findInPath("sh") != null) {
			String start = sh.getAbsolutePath();
			ProcessBuilder process = new ProcessBuilder("sh", start);
			process.redirectErrorStream(true);
			return process.start();
		} else if (bat.exists() && findInPath("cmd.exe") != null) {
			String start = bat.getAbsolutePath();
			ProcessBuilder process = new ProcessBuilder("cmd.exe", "/c", start);
			process.redirectErrorStream(true);
			return process.start();
		} else {
			return null;
		}
	}

	private File findInPath(String exe) {
		String systemPath = System.getenv("PATH");
		for (String path : systemPath.split(File.pathSeparator)) {
			File file = new File(path, exe);
			try {
				if (file.exists())
					return file;
			} catch (SecurityException e) {
				continue;
			}
		}
		return null;
	}

}
