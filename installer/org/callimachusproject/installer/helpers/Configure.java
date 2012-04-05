package org.callimachusproject.installer.helpers;

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
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

public class Configure {
	private static final String LOGGING_PROPERTIES = "logging.properties";
	private static final String MAIL_PROPERTIES = "mail.properties";
	private static final String SERVER_CONF = "callimachus.conf";
	private static final String START_SCRIPT = "callimachus-start";
	private static final String STOP_SCRIPT = "callimachus-stop";
	private static final Map<String,Configure> instances = new HashMap<String, Configure>();

	public static Configure getInstance(String installPath) {
		synchronized (instances) {
			File dir = new File(installPath);
			String key = dir.getAbsolutePath();
			Configure c = instances.get(key);
			if (c == null) {
				instances.put(key, c = new Configure(dir));
			}
			return c;
		}
	}

	private final File dir;
	private final ThreadFactory tfactory = new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "Callimachus-Configure-Setup-Queue");
			t.setDaemon(true);
			return t;
		}
	};
	private final ExecutorService executor = Executors
			.newSingleThreadScheduledExecutor(tfactory);
	private volatile Exception exception;
	/** Only access setup through executor */
	private SetupProxy setup;
	private boolean connected;

	private Configure(File dir) {
		this.dir = dir;
	}

	public Properties getServerConfiguration() throws IOException {
		File file = new File(new File(dir, "etc"), SERVER_CONF);
		Properties properties = new Properties();
		InputStream in = openServerConfiguration(file);
		if (in != null) {
			try {
				BufferedReader rd = new BufferedReader(new InputStreamReader(in));
				String line;
				while ((line = rd.readLine()) != null) {
					if (line.matches("^\\s*#.*$"))
						continue;
					int eq = line.indexOf('=');
					if (eq < 0)
						continue;
					String name = line.substring(0, eq).trim();
					String value = line.substring(eq + 1).trim();
					if (value.startsWith("\"") && value.endsWith("\"")) {
						value = value.substring(1, value.length() - 1);
					}
					properties.setProperty(name, value);
				}
			} finally {
				in.close();
			}
		}
		return properties;
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
				bw.write(key + "=\"" + val + "\"");
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
		file.setReadable(false, false);
		file.setReadable(true, true);
		file.setWritable(false, false);
		file.setWritable(true, true);
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
		return launch(STOP_SCRIPT);
	}

	public boolean startServer() throws Exception {
		return launch(START_SCRIPT);
	}

	public boolean primeServer(String origin) throws InterruptedException {
		for (int i = 0; i < 120; i++) {
			try {
				request(origin + "/");
				request(origin + "/callimachus/scripts/web_bundle?source");
				return true;
			} catch (FileNotFoundException e) {
				return true;
			} catch (IOException e) {
				Thread.sleep(2000);
			}
		}
		return false;
	}

	public boolean isWebBrowserSupported() {
		if (!Desktop.isDesktopSupported())
			return false;
        Desktop desktop = Desktop.getDesktop();
        return desktop.isSupported(Desktop.Action.BROWSE);
	}

	public boolean openWebBrowser(String url) {
		if (!isWebBrowserSupported())
			return false;
		try {
			java.awt.Desktop.getDesktop().browse(URI.create(url));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public File createBackup() throws IOException, DatatypeConfigurationException {
		if (!dir.isDirectory())
			return null;
		String name = dir.getName() + "_" + timeStamp() + ".zip";
		File backup = new File(new File(dir, "backups"), name);
		boolean created = false;
		backup.getParentFile().mkdirs();
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backup));
		try {
			String base = dir.getAbsolutePath() + "/";
			created |= putEntries(base, new File(dir, "etc"), zos);
			created |= putEntries(base, new File(dir, "repositories"), zos);
			created |= putEntries(base, new File(dir, "www"), zos);
			created |= putEntries(base, new File(dir, "blob"), zos);
		} finally {
			zos.close();
		}
		if (created)
			return backup;
		backup.delete();
		return null;
	}

	public List<String> getDefaultOrigins(String ports) throws SocketException {
		Collection<String> hosts = getAllCanonicalHostNames();
		Set<String> numbers = new LinkedHashSet<String>(Arrays.asList(ports
				.split("\\s+")));
		numbers.remove("");
		List<String> result = new ArrayList<String>(hosts.size()
				* numbers.size());
		for (String host : hosts) {
			if (host.matches("^[a-zA-Z0-9.-]+$")) {
				for (String port : numbers) {
					if ("80".equals(port)) {
						result.add("http://" + host.toLowerCase());
					} else if ("443".equals(port)) {
						result.add("https://" + host.toLowerCase());
					} else {
						result.add("http://" + host.toLowerCase() + ":" + port);
					}
				}
			}
		}
		return result;
	}

	public boolean isError() {
		return exception != null;
	}

	public synchronized boolean isIdle(long timeout, TimeUnit unit) throws InterruptedException {
		try {
			Future<Void> task = perform(new Callable<Void>() {
				public Void call() throws Exception {
					return null;
				}
			});
			Thread.yield();
			task.get(timeout, unit);
			return true;
		} catch (ExecutionException e) {
			throw new AssertionError(e);
		} catch (TimeoutException e) {
			return false;
		}
	}

	public synchronized boolean isConnected() {
		return connected;
	}

	public synchronized Future<Void> connect(final URL ctemplate,
			final Map<String, String> parameters) {
		connected = true;
		return perform(new Callable<Void>() {
			public Void call() throws Exception {
				File lib = new File(dir, "lib");
				setup = new SetupProxy(lib);
				setup.connect(dir,
						new ConfigTemplate(ctemplate).render(parameters));
				return null;
			}
		});
	}

	public synchronized void disconnect() throws Exception {
		connected = false;
		Future<Void> task = executor.submit(new Callable<Void>() {
			public Void call() throws Exception {
				try {
					setup.disconnect();
					if (exception != null)
						throw exception;
					return null;
				} finally {
					exception = null;
				}
			}
		});
		try {
			task.get();
		} catch (ExecutionException e) {
			try {
				throw e.getCause();
			} catch (Exception e1) {
				throw e1;
			} catch (Throwable e1) {
				throw e;
			}
		}
	}

	public void createOrigin(final String origin) throws Exception {
		perform(new Callable<Void>() {
			public Void call() throws Exception {
				setup.createOrigin(origin, getCallimachusCarUrl());
				return null;
			}
		});
	}

	public void createVirtualHost(final String virtual, final String origin)
			throws Exception {
		perform(new Callable<Void>() {
			public Void call() throws Exception {
				setup.createVirtualHost(virtual, origin);
				return null;
			}
		});
	}

	public void createRealm(final String realm, final String origin)
			throws Exception {
		perform(new Callable<Void>() {
			public Void call() throws Exception {
				setup.createRealm(realm, origin);
				return null;
			}
		});
	}

	public void setServeAllResourcesAs(final String origin) throws Exception {
		perform(new Callable<Void>() {
			public Void call() throws Exception {
				setup.setServeAllResourcesAs(origin);
				return null;
			}
		});
	}

	public void createAdmin(final String name, final String email,
			final String username, final String password, final String origin)
			throws Exception {
		perform(new Callable<Void>() {
			public Void call() throws Exception {
				setup.createAdmin(name, email, username, password, origin);
				return null;
			}
		});
	}

	private InputStream openServerConfiguration(File file)
			throws FileNotFoundException {
		if (file.exists())
			return new FileInputStream(file);
		ClassLoader cl = getClass().getClassLoader();
		return cl.getResourceAsStream("META-INF/templates/callimachus.conf");
	}

	private void request(String url) throws IOException,
			MalformedURLException {
		InputStream in = new URL(url).openStream();
		try {
			byte[] buf = new byte[1024];
			while (in.read(buf) >= 0)
				;
		} finally {
			in.close();
		}
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
				set.remove(raddr.getHostAddress());
			}
			Enumeration<NetworkInterface> virtualIfaces = iface
					.getSubInterfaces();
			while (virtualIfaces.hasMoreElements()) {
				NetworkInterface viface = virtualIfaces.nextElement();
				Enumeration<InetAddress> vaddrs = viface.getInetAddresses();
				while (vaddrs.hasMoreElements()) {
					InetAddress vaddr = vaddrs.nextElement();
					set.add(vaddr.getCanonicalHostName());
					set.remove(vaddr.getHostAddress());
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

	private boolean launch(String script) throws Exception {
		Process p = exec(script);
		if (p == null)
			return false;
		transfer(p.getInputStream(), System.err);
		transfer(p.getErrorStream(), System.err);
		p.getOutputStream().close();
		return p.waitFor() == 0;
	}

	private void transfer(final InputStream in, final PrintStream out) {
		if (in != null) {
			tfactory.newThread(new Runnable() {
				public void run() {
					try {
						try {
							int read;
							byte[] buf = new byte[1024];
							while ((read = in.read(buf)) >= 0) {
								out.write(buf, 0, read);
							}
						} finally {
							in.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
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

	private String timeStamp() throws DatatypeConfigurationException {
		GregorianCalendar now = new GregorianCalendar(
				TimeZone.getTimeZone("UTC"));
		DatatypeFactory df = DatatypeFactory.newInstance();
		String stamp = df.newXMLGregorianCalendar(now).toXMLFormat();
		return stamp.replaceAll("[^0-9]", "");
	}

	private boolean putEntries(String base, File file, ZipOutputStream zos)
			throws IOException {
		if (file.isFile()) {
			String path = file.getAbsolutePath();
			String name = path;
			if (name.startsWith(base)) {
				name = name.substring(base.length());
			}
			ZipEntry entry = new ZipEntry(path);
			entry.setTime(file.lastModified());
			entry.setSize(file.length());
			zos.putNextEntry(entry);
			FileInputStream fis = new FileInputStream(file);
			try {
				int read = 0;
				byte[] buf = new byte[2156];
				while ((read = fis.read(buf)) != -1) {
					zos.write(buf, 0, read);
				}
				return true;
			} finally {
				fis.close();
			}
		} else {
			File[] listFiles = file.listFiles();
			if (listFiles == null)
				return false;
			boolean content = false;
			for (File f : listFiles) {
				content |= putEntries(base, f, zos);
			}
			return content;
		}
	}

	private synchronized Future<Void> perform(final Callable<Void> task) {
		if (exception != null)
			return null;
		return executor.submit(new Callable<Void>() {
			public Void call() throws Exception {
				try {
					return task.call();
				} catch (Exception e) {
					exception = e;
					e.printStackTrace();
					throw e;
				}
			}
		});
	}

}
