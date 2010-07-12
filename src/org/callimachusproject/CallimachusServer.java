package org.callimachusproject;

import static java.lang.Integer.toHexString;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.activation.MimeTypeParseException;
import javax.activation.MimetypesFileTypeMap;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.openrdf.http.object.HTTPObjectServer;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.http.object.util.FileUtil;
import org.openrdf.http.object.util.NamedThreadFactory;
import org.openrdf.http.object.util.SharedExecutors;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallimachusServer {
	private static final Charset ASCII = Charset.forName("US-ASCII");
	private static final Charset DEFAULT = Charset.defaultCharset();
	private static final String NS = "http://callimachusproject.org/rdf/2009/framework#";
	private static ScheduledExecutorService executor = SharedExecutors
			.getTimeoutThreadPool();
	private static final String ENVELOPE_TYPE = "message/x-response";
	private static final String IDENTITY_PATH = "/diverted;";
	/** Date format pattern used to generate the header in RFC 1123 format. */
	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	/** The time zone to use in the date header. */
	public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static NamedThreadFactory threads = new NamedThreadFactory(
			"Webaps Listener", true);
	private Logger logger = LoggerFactory.getLogger(CallimachusServer.class);
	private MimetypesFileTypeMap mimetypes;
	private final DateFormat dateformat;
	private int reloading;
	private String authority;
	private String basic;
	private String authorization;
	private File webappsDir;
	private File entriesDir;
	private ObjectRepository repository;
	private HTTPObjectServer server;
	private String origin;
	private WebAppListener listener;
	private int port;
	private boolean conditional = true;
	private PrintStream out;

	public CallimachusServer(Repository repository, File dataDir, File webapps)
			throws Exception {
		dateformat = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
		dateformat.setTimeZone(GMT);
		basic = "boot:" + generatePassword();
		webappsDir = webapps.getCanonicalFile();
		entriesDir = new File(dataDir, "entries").getCanonicalFile();
		entriesDir.mkdirs();
		this.repository = importJars(webappsDir, repository);
		ObjectConnection con = this.repository.getConnection();
		try {
			ClassLoader cl = con.getObjectFactory().getClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(cl);
			} catch (SecurityException e) {
				logger.warn(e.toString(), e);
			}
			mimetypes = createMimetypesMap();
		} finally {
			con.close();
		}
		server = createServer(dataDir, basic, this.repository);
	}

	public void printStatus(PrintStream out) {
		this.out = out;
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
		String prefix = "http://" + authority + IDENTITY_PATH;
		server.setIdentityPrefix(new String[] { prefix });
	}

	public String getServerName() {
		return server.getName();
	}

	public void setServerName(String serverName) {
		server.setName(serverName);
	}

	public File getWebappsDir() {
		return webappsDir;
	}

	public ObjectRepository getRepository() {
		return repository;
	}

	public boolean isConditionalRequests() {
		return conditional;
	}

	public void setConditionalRequests(boolean conditional) {
		this.conditional = conditional;
	}

	public void listen(int... ports) throws Exception {
		assert ports != null && ports.length > 0;
		this.port = ports[0];
		if (authority == null) {
			setAuthority(getAuthority(port));
		}
		origin = "http://" + authority + "/";
		byte[] decoded = basic.getBytes();
		String cred = new String(Base64.encodeBase64(decoded), "8859_1");
		authorization = "Basic " + cred;
		server.listen(ports);
	}

	public void start() throws Exception {
		logger.info("Callimachus is binding to {}", origin);
		InetSocketAddress host = getAuthorityAddress();
		HTTPObjectClient.getInstance().setProxy(host, server);
		boolean empty = false;
		if (!conditional || (empty = isEmpty(repository))) {
			repository.setCompileRepository(false);
		}
		uploadWebapps(conditional && !empty);
		repository.setCompileRepository(true);
		server.start();
		started();
		System.gc();
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public void stop() throws Exception {
		stopping();
		InetSocketAddress host = getAuthorityAddress();
		HTTPObjectClient.getInstance().removeProxy(host, server);
		if (listener != null) {
			listener.stop();
		}
		server.stop();
	}

	public void destroy() throws Exception {
		server.destroy();
	}

	private InetSocketAddress getAuthorityAddress() {
		InetSocketAddress host;
		if (authority.contains(":")) {
			int idx = authority.indexOf(':');
			int port = Integer.parseInt(authority.substring(idx + 1));
			host = new InetSocketAddress(authority.substring(0, idx), port);
		} else {
			host = new InetSocketAddress(authority, 80);
		}
		return host;
	}

	private void uploadWebapps(boolean conditional) throws IOException,
			JNotifyException, InterruptedException, ExecutionException {
		print("Uploading Webapps");
		try {
			Thread listenerThread = null;
			int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED
					| JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;
			try {
				WebAppListener wal = new WebAppListener();
				JNotify
						.addWatch(webappsDir.getCanonicalPath(), mask, true,
								wal);
				listenerThread = threads.newThread(wal);
				listener = wal;
			} catch (NoClassDefFoundError e) {
				logger.error(e.getMessage());
			} catch (UnsatisfiedLinkError e) {
				logger.error(e.getMessage());
			}
			if (!conditional) {
				deleteMissingFiles();
			}
			uploadWebApps(webappsDir, "", conditional);
			if (listener != null && listenerThread != null) {
				listenerThread.start();
				listener.await();
			}
		} finally {
			println();
		}
	}

	private final class CharsetDetector implements nsICharsetDetectionObserver {
		private Charset charset;

		public Charset detect(File file, boolean gzip) throws IOException {
			InputStream in = new FileInputStream(file);
			if (gzip) {
				in = new GZIPInputStream(in);
			}
			try {
				return detect(in);
			} finally {
				in.close();
			}
		}

		public Charset detect(InputStream in) throws IOException {
			boolean ascii = true;
			nsDetector det = new nsDetector();
			det.Init(this);
			int len;
			boolean done = false;
			byte[] buf = new byte[1024];
			while ((len = in.read(buf)) >= 0) {
				// Check if the stream is only ascii.
				if (ascii) {
					ascii = det.isAscii(buf, len);
				}
				// DoIt if non-ascii and not done yet.
				if (!ascii && !done) {
					done = det.DoIt(buf, len, false);
				}
			}
			det.Done();
			if (charset == null && ascii) {
				charset = ASCII;
			} else if (charset == null) {
				for (String name : det.getProbableCharsets()) {
					try {
						if ("nomatch".equals(name))
							continue;
						Charset cs = Charset.forName(name);
						if (charset == null) {
							charset = cs;
						}
						if (DEFAULT.contains(cs))
							return DEFAULT;
						if (cs.contains(DEFAULT))
							return cs;
					} catch (IllegalCharsetNameException e) {
						logger.warn(e.toString(), e);
					} catch (UnsupportedCharsetException e) {
						logger.warn(e.toString(), e);
					}
				}
			}
			if (charset == null || DEFAULT.contains(charset))
				return DEFAULT;
			return charset;
		}

		public void Notify(String charset) {
			if (charset == null) {
				this.charset = null;
			} else {
				try {
					this.charset = Charset.forName(charset);
				} catch (IllegalCharsetNameException e) {
					logger.warn(e.toString(), e);
				} catch (UnsupportedCharsetException e) {
					logger.warn(e.toString(), e);
				}
			}
		}
	}

	private final class WebAppListener implements JNotifyListener, Runnable {
		private Queue<File> queue = new LinkedList<File>();
		private File eos, wait;

		public WebAppListener() throws IOException {
			eos = File.createTempFile("callimachus", "eos");
			wait = File.createTempFile("callimachus", "wait");
			eos.delete();
			wait.delete();
		}

		public void stop() {
			synchronized (queue) {
				queue.add(eos);
				queue.notifyAll();
			}
		}

		public void fileCreated(int wd, String rootPath, String name) {
			fileModified(wd, rootPath, name);
		}

		public void fileRenamed(int wd, String rootPath, String oldName,
				String newName) {
			fileDeleted(wd, rootPath, oldName);
			fileModified(wd, rootPath, newName);
		}

		public void fileDeleted(int wd, String rootPath, String name) {
			fileModified(wd, rootPath, name);
		}

		public void fileModified(int wd, String rootPath, String name) {
			if (name == null || name.contains("/WEB-INF/"))
				return;
			File file = new File(new File(rootPath), name).getAbsoluteFile();
			if (!isHidden(file)) {
				add(file);
			}
		}

		public void await() throws InterruptedException {
			synchronized (queue) {
				while (!queue.isEmpty()) {
					queue.wait();
				}
			}
		}

		@Override
		public void run() {
			File file;
			while ((file = take()) != null) {
				try {
					String path = getWebPath(file);
					if (file.exists()) {
						reloading();
						uploadWebApps(file, path, false);
						reloaded();
					} else {
						reloading();
						deleteFile(path, file);
						reloaded();
					}
				} catch (Exception e) {
					logger.error(e.toString());
				}
			}
		}

		private void add(File file) {
			synchronized (queue) {
				queue.remove(file);
				if (queue.isEmpty()) {
					queue.add(wait);
				}
				queue.add(file);
				queue.notifyAll();
			}
		}

		private File take() {
			File file;
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						return null;
					}
				}
				file = queue.remove();
				queue.notifyAll();
			}
			if (file.equals(wait)) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return null;
				}
				return take();
			}
			if (file.equals(eos))
				return null;
			return file;
		}
	}

	private boolean isEmpty(ObjectRepository repository)
			throws RepositoryException {
		ObjectConnection con = repository.getConnection();
		try {
			return con.isEmpty();
		} finally {
			con.close();
		}
	}

	private String getAuthority(int port) throws IOException {
		String hostname;
		try {
			// attempt for the host canonical host name
			hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException uhe) {
			try {
				// attempt to get the loop back address
				hostname = InetAddress.getByName(null).getCanonicalHostName();
			} catch (UnknownHostException uhe2) {
				// default to a standard loop back IP
				hostname = "127.0.0.1";
			}
		}
		if (port == 80)
			return hostname;
		return hostname + ":" + port;
	}

	private String generatePassword() {
		return Long.toHexString(new Random(System.nanoTime()).nextLong());
	}

	private HTTPObjectServer createServer(File dir, String basic,
			ObjectRepository or) throws IOException, MimeTypeParseException {
		File wwwDir = new File(dir, "www");
		File cacheDir = new File(dir, "cache");
		FileUtil.deleteOnExit(cacheDir);
		File out = new File(cacheDir, "server");
		HTTPObjectServer server = new HTTPObjectServer(or, wwwDir, out, basic);
		server.setEnvelopeType(ENVELOPE_TYPE);
		HTTPObjectClient.getInstance().setEnvelopeType(ENVELOPE_TYPE);
		return server;
	}

	private ObjectRepository importJars(File webappsDir, Repository repository)
			throws URISyntaxException, RepositoryConfigException,
			RepositoryException, IOException {
		File dataDir = repository.getDataDir();
		File lib;
		File ontologies;
		if (dataDir == null) {
			lib = File.createTempFile("lib", "");
			ontologies = File.createTempFile("ontologies", "");
			lib.delete();
			ontologies.delete();
		} else {
			lib = new File(dataDir, "lib");
			ontologies = new File(dataDir, "ontologies");
		}
		lib.mkdirs();
		ontologies.mkdirs();
		FileUtil.deleteOnExit(ontologies);
		List<URL> jars = findJars(webappsDir, false, false, false, lib,
				ontologies, new ArrayList<URL>());
		ObjectRepository or;
		if (jars.isEmpty() && repository instanceof ObjectRepository) {
			or = (ObjectRepository) repository;
		} else {
			ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
			ObjectRepositoryConfig config = factory.getConfig();
			if (!jars.isEmpty()) {
				for (URL jar : jars) {
					if (jar.toExternalForm().endsWith(".jar")
							|| jar.toExternalForm().endsWith(".JAR")) {
						config.addConceptJar(jar);
					} else {
						config.addImports(jar);
					}
				}
			}
			or = factory.createRepository(config, repository);
		}
		return or;
	}

	private List<URL> findJars(File dir, boolean web, boolean inlib, boolean ont,
			File lib, File ontologies, ArrayList<URL> jars) throws IOException {
		if (isHidden(dir))
			return jars;
		String name = dir.getName();
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				boolean webinf = name.equalsIgnoreCase("WEB-INF");
				boolean eqlib = name.equalsIgnoreCase("lib");
				boolean ent = name.equalsIgnoreCase("ontologies");
				findJars(file, webinf, inlib || web && eqlib, ont || web
						&& ent, lib, ontologies, jars);
			}
			if (web && name.equalsIgnoreCase("classes")) {
				jars.add(asJar(dir, lib).toURI().toURL());
			}
		} else if (inlib && (name.endsWith(".jar") || name.endsWith(".JAR"))) {
			jars.add(asJar(dir, lib).toURI().toURL());
		} else if (ont) {
			int c = dir.getAbsolutePath().hashCode();
			String hex = toHexString(Math.abs(c));
			File dest = new File(ontologies, hex + dir.getName());
			jars.add(copy(dir, dest).toURI().toURL());
		} else if (name.endsWith(".war") || name.endsWith(".WAR")) {
			String cpath = dir.getAbsolutePath() + "!/WEB-INF/classes/";
			String code = toHexString(cpath.hashCode());
			File classesFile = new File(lib, dir.getName() + code + ".jar");
			JarOutputStream classes = null;
			ZipFile zip = new ZipFile(dir);
			try {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (isHidden(entry.getName()))
						continue;
					String path = entry.getName().replace('\\', '/');
					if (path.endsWith("/"))
						continue;
					while (path.startsWith("/")) {
						path = path.substring(1);
					}
					if (path.length() < 0)
						continue;
					if (path.startsWith("WEB-INF/lib/")) {
						String fullpath = dir.getAbsolutePath() + "!/" + path;
						jars.add(asJar(zip, entry, dir, fullpath, lib).toURI()
								.toURL());
					} else if (path.startsWith("WEB-INF/classes/")) {
						if (classes == null) {
							classes = new JarOutputStream(new FileOutputStream(
									classesFile));
						}
						String subentry = path.substring("WEB-INF/classes/"
								.length());
						classes.putNextEntry(new JarEntry(subentry));
						InputStream in = zip.getInputStream(entry);
						try {
							int read;
							byte[] buf = new byte[1024];
							while ((read = in.read(buf)) >= 0) {
								classes.write(buf, 0, read);
							}
							classes.closeEntry();
						} finally {
							in.close();
						}
					} else if (path.startsWith("WEB-INF/ontologies/")) {
						String en = path;
						if (en.contains("/")) {
							en = en.substring(en.lastIndexOf('/') + 1);
						}
						String fullpath = dir.getAbsolutePath() + "!/" + path;
						String hex = toHexString(Math.abs(fullpath.hashCode()));
						File dest = new File(ontologies, hex + en);
						jars.add(copy(zip, entry, dest).toURI().toURL());
					}
				}
			} finally {
				zip.close();
			}
			if (classes != null) {
				classes.close();
				jars.add(classesFile.toURI().toURL());
			}
		}
		return jars;
	}

	private File asJar(ZipFile zip, ZipEntry entry, File dir, String fullpath,
			File dest) throws IOException {
		String code = toHexString(fullpath.hashCode());
		File jar = new File(dest, dir.getName() + code + ".jar");
		return copy(zip, entry, jar);
	}

	private File copy(ZipFile zip, ZipEntry entry, File dest)
			throws IOException, FileNotFoundException {
		InputStream stream = zip.getInputStream(entry);
		ReadableByteChannel in = Channels.newChannel(stream);
		try {
			FileChannel out = new FileOutputStream(dest).getChannel();
			try {
				out.transferFrom(in, 0, entry.getSize());
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
		return dest;
	}

	private File asJar(File jar, File dir) throws IOException {
		String code = toHexString(jar.getAbsolutePath().hashCode());
		File dest = new File(dir, jar.getName() + code + ".jar");
		if (jar.isDirectory()) {
			dest = zip(jar, dest);
		} else {
			dest = copy(jar, dest);
		}
		return dest;
	}

	private File copy(File jar, File file) throws IOException {
		FileChannel in = new FileInputStream(jar).getChannel();
		try {
			FileChannel out = new FileOutputStream(file).getChannel();
			try {
				in.transferTo(0, jar.length(), out);
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
		return file;
	}

	private File zip(File dir, File zip) throws IOException {
		JarOutputStream zout = new JarOutputStream(new FileOutputStream(zip));
		try {
			addDirectory("", dir, zout);
			return zip;
		} finally {
			zout.close();
		}
	}

	private static void addDirectory(String prefix, File fileSource,
			JarOutputStream zout) throws IOException {
		File[] files = fileSource.listFiles();
		for (int i = 0; i < files.length; i++) {
			String entry = files[i].getName();
			if (prefix.length() > 0) {
				entry = prefix + "/" + entry;
			}
			if (files[i].isDirectory()) {
				addDirectory(entry, files[i], zout);
			} else {
				byte[] buffer = new byte[1024];
				FileInputStream fin = new FileInputStream(files[i]);
				try {
					zout.putNextEntry(new ZipEntry(entry));
					int length;
					while ((length = fin.read(buffer)) > 0) {
						zout.write(buffer, 0, length);
					}
					zout.closeEntry();
				} finally {
					fin.close();
				}
			}
		}
	}

	private void started() throws GatewayTimeout, IOException,
			InterruptedException {
		InetSocketAddress server = new InetSocketAddress(InetAddress
				.getLocalHost(), port);
		HttpRequest req = new BasicHttpRequest("POST", NS + "boot?started");
		req.setHeader("Authorization", authorization);
		HttpResponse resp = HTTPObjectClient.getInstance().service(server, req);
		StatusLine status = resp.getStatusLine();
		if (status.getStatusCode() != 204) {
			logger.error(status.getReasonPhrase() + " once started");
		}
	}

	private synchronized void reloading() throws GatewayTimeout, IOException,
			InterruptedException {
		reloading++;
	}

	private synchronized void reloaded() throws GatewayTimeout, IOException,
			InterruptedException {
		final int loaded = reloading;
		executor.schedule(new Runnable() {
			public void run() {
				try {
					if (reloading == loaded) {
						InetSocketAddress server = new InetSocketAddress(
								InetAddress.getLocalHost(), port);
						HttpRequest req = new BasicHttpRequest("POST", NS
								+ "boot?reloaded");
						req.setHeader("Authorization", authorization);
						HttpResponse resp = HTTPObjectClient.getInstance()
								.service(server, req);
						StatusLine status = resp.getStatusLine();
						System.gc();
						println();
						if (status.getStatusCode() != 204) {
							logger.error(status.getReasonPhrase()
									+ " once reloaded");
						}
					}
				} catch (Exception e) {
					logger.error(e.toString());
				}
			}
		}, 2, TimeUnit.SECONDS);
	}

	private void stopping() throws GatewayTimeout, IOException,
			InterruptedException {
		InetSocketAddress server = new InetSocketAddress(InetAddress
				.getLocalHost(), port);
		HttpRequest req = new BasicHttpRequest("POST", NS + "boot?stopping");
		req.setHeader("Authorization", authorization);
		HttpResponse resp = HTTPObjectClient.getInstance().service(server, req);
		StatusLine status = resp.getStatusLine();
		if (status.getStatusCode() != 204) {
			logger.error(status.getReasonPhrase() + " while stopping");
		}
	}

	private void deleteMissingFiles() throws IOException, InterruptedException,
			ExecutionException {
		for (String entry : getEntries(webappsDir)) {
			File file = new File(entry);
			if (!file.exists()) {
				deleteFile(getWebPath(file), file);
			}
		}
		clearEntries(webappsDir);
	}

	private void uploadWebApps(File file, String path, boolean conditional)
			throws InterruptedException, ExecutionException, IOException {
		String name = file.getName();
		if (isHidden(file) || "WEB-INF".equals(name))
			return;
		boolean gzip = name.endsWith(".gz");
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				uploadWebApps(f, getPath(path, f.getName()), conditional);
			}
		} else if (file.getName().endsWith(".war")
				|| file.getName().endsWith(".WAR")) {
			if (path.endsWith(".war") || path.endsWith(".WAR")) {
				path = path.substring(0, path.length() - 4);
			}
			uploadWarFile(file, path, conditional);
		} else {
			HttpResponse resp = upload(file, path, gzip, conditional);
			addEntries(webappsDir, Collections.singleton(file.getAbsolutePath()));
			report(resp, path, file.getName());
		}
	}

	private HttpResponse upload(File file, String path, boolean gzip,
			boolean conditional) throws IOException, InterruptedException,
			ExecutionException {
		long size = file.length();
		String type = getContentType(file.getName(), gzip);
		if (type.startsWith("text/") && !type.contains("charset")) {
			type += ";charset=" + detectCharset(file, gzip).name();
		}
		long modified = file.lastModified();
		String since = dateformat.format(new Date(modified));
		BasicHttpEntityEnclosingRequest req;
		req = new BasicHttpEntityEnclosingRequest("PUT", origin + path);
		req.setHeader("Authorization", authorization);
		if (conditional) {
			req.setHeader("If-Unmodified-Since", since);
		}
		req.setHeader("Content-Type", type);
		req.setHeader("Content-Length", Long.toString(size));
		if (gzip) {
			req.setHeader("Content-Encoding", "gzip");
			logger.debug("{}\tcompressed {}", path, type);
		} else {
			logger.debug("{}\t{}", path, type);
		}
		req.setEntity(new FileEntity(file, type));
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.service(req);
		if (size != file.length() || modified != file.lastModified()) {
			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
			return upload(file, path, gzip, false);
		}
		return resp;
	}

	private void uploadWarFile(File file, String path, boolean conditional)
			throws ZipException, IOException, InterruptedException,
			ExecutionException {
		ZipFile zip = new ZipFile(file);
		try {
			Set<String> names = new HashSet<String>();
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				String entry = entries.nextElement().getName();
				if (!isWebResource(entry))
					continue;
				names.add(entry);
			}
			for (String entry : getEntries(file)) {
				if (!names.contains(entry)) {
					deleteWarEntry(file, path, entry);
				}
			}
			setEntries(file, names);
			for (String entry : names) {
				String p = entry.replace('\\', '/');
				if (p.length() < 0 || p.endsWith("/"))
					continue;
				String ep = path;
				String name = entry;
				int idx = p.lastIndexOf('/');
				if (idx >= 0) {
					ep = ep + "/" + p.substring(0, idx);
					name = p.substring(idx + 1);
				}
				ZipEntry ze = zip.getEntry(entry);
				if (ze == null)
					continue;
				String dest = getPath(ep, name);
				HttpResponse resp = uploadEntry(zip, ze, dest, conditional);
				report(resp, path, file.getName() + "!" + entry);
			}
		} finally {
			zip.close();
		}
	}

	private HttpResponse uploadEntry(ZipFile zip, ZipEntry entry, String path,
			boolean conditional) throws IOException {
		long size = entry.getSize();
		String type = getContentType(entry.getName(), false);
		if (type.startsWith("text/") && !type.contains("charset")) {
			InputStream in = zip.getInputStream(entry);
			try {
				type += ";charset=" + new CharsetDetector().detect(in).name();
			} finally {
				in.close();
			}
		}
		long modified = entry.getTime();
		String since = dateformat.format(new Date(modified));
		BasicHttpEntityEnclosingRequest req;
		req = new BasicHttpEntityEnclosingRequest("PUT", origin + path);
		req.setHeader("Authorization", authorization);
		if (conditional) {
			req.setHeader("If-Unmodified-Since", since);
		}
		req.setHeader("Content-Type", type);
		req.setHeader("Content-Length", Long.toString(size));
		logger.debug("{}\t{}", path, type);
		InputStream in = zip.getInputStream(entry);
		req.setEntity(new InputStreamEntity(in, entry.getSize()));
		try {
			return HTTPObjectClient.getInstance().service(req);
		} finally {
			in.close();
		}
	}

	private void report(HttpResponse resp, String path, String filename)
			throws IOException {
		int status = resp.getStatusLine().getStatusCode();
		HttpEntity entity = resp.getEntity();
		if (status == 412) {
			print(".");
			if (entity != null) {
				entity.consumeContent();
			}
		} else if (status == 204) {
			print("^");
			if (entity != null) {
				entity.consumeContent();
			}
		} else {
			print("!");
			logger.error(resp.getStatusLine().getReasonPhrase() + " for "
					+ filename);
			if (entity != null) {
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					entity.writeTo(out);
					logger.debug(out.toString());
				} finally {
					entity.consumeContent();
				}
			}
		}
	}

	private String getWebPath(File file) throws IOException {
		String rootPath = webappsDir.getCanonicalPath()
				+ File.separator;
		String path = file.getAbsolutePath();
		if (path.startsWith(rootPath)) {
			path = path.substring(rootPath.length());
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		int idx = path.lastIndexOf('/');
		if (idx < 0)
			return getPath("", path);
		String directory = path.substring(0, idx);
		String name = path.substring(idx + 1);
		return getPath(directory, name);
	}

	private String getPath(String directory, String name) {
		boolean gzip = name.endsWith(".gz");
		name = gzip ? name.substring(0, name.length() - 3) : name;
		if (name.startsWith("index.") && directory.length() == 0)
			return "";
		if (name.startsWith("index."))
			return directory + "/";
		if (directory.length() == 0)
			return name.replace(' ', '+');
		return directory + "/" + name.replace(' ', '+');
	}

	private MimetypesFileTypeMap createMimetypesMap() {
		MimetypesFileTypeMap mimetypes = new MimetypesFileTypeMap();
		for (RDFFormat format : RDFFormat.values()) {
			boolean extensionPresent = false;
			StringBuilder sb = new StringBuilder();
			sb.append(format.getDefaultMIMEType());
			for (String ext : format.getFileExtensions()) {
				if ("application/octet-stream".equals(mimetypes
						.getContentType("file." + ext))) {
					extensionPresent = true;
					sb.append(" ").append(ext);
				}
			}
			if (extensionPresent) {
				mimetypes.addMimeTypes(sb.toString());
			}
		}
		return mimetypes;
	}

	private String getContentType(String filename, boolean gzip) {
		if (gzip) {
			filename = filename.replaceAll(".gz$", "");
		}
		return mimetypes.getContentType(filename);
	}

	private Charset detectCharset(File file, boolean gzip) throws IOException {
		return new CharsetDetector().detect(file, gzip);
	}

	private void deleteFile(String path, File file) throws IOException,
			InterruptedException, ExecutionException {
		deleteFile(path, file.getName());
		if (path.endsWith(".war") || path.endsWith(".WAR")) {
			String wp = path.substring(0, path.length() - 4);
			for (String entry : getEntries(file)) {
				deleteWarEntry(file, wp, entry);
			}
			clearEntries(file);
		}
	}

	private synchronized Collection<String> getEntries(File file) throws IOException {
		File entriesFile = getEntriesFile(file);
		Set<String> set = new HashSet<String>();
		if (entriesFile.exists()) {
			String line;
			BufferedReader reader;
			reader = new BufferedReader(new FileReader(entriesFile));
			try {
				while ((line = reader.readLine()) != null) {
					set.add(line);
				}
			} finally {
				reader.close();
			}
		}
		return set;
	}

	private synchronized void addEntries(File file, Collection<String> names)
			throws IOException {
		Collection<String> entries = getEntries(file);
		entries.addAll(names);
		setEntries(file, names);
	}

	private synchronized void setEntries(File file, Collection<String> names)
			throws IOException {
		PrintWriter writer;
		File entriesFile = getEntriesFile(file);
		writer = new PrintWriter(new FileWriter(entriesFile));
		try {
			for (String line : names) {
				writer.println(line);
			}
		} finally {
			writer.close();
		}
	}

	private synchronized void clearEntries(File file) throws IOException {
		getEntriesFile(file).delete();
	}

	private File getEntriesFile(File file) {
		int code = file.getAbsolutePath().hashCode();
		String name = file.getName() + toHexString(code) + ".list";
		File entriesFile = new File(entriesDir, name);
		return entriesFile;
	}

	private void deleteWarEntry(File file, String path, String entry)
			throws IOException, InterruptedException, ExecutionException {
		String p = entry.replace('\\', '/');
		if (p.length() > 0 && !p.endsWith("/")) {
			String ep = path;
			String name = entry;
			int idx = p.lastIndexOf('/');
			if (idx >= 0) {
				ep = ep + "/" + p.substring(0, idx);
				name = p.substring(idx + 1);
			}
			deleteFile(getPath(ep, name), file.getName() + "!" + entry);
		}
	}

	private void deleteFile(String path, String filename) throws IOException,
			InterruptedException, ExecutionException {
		BasicHttpRequest req = new BasicHttpRequest("DELETE", origin + path);
		req.setHeader("Authorization", authorization);
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.service(req);
		int status = resp.getStatusLine().getStatusCode();
		HttpEntity entity = resp.getEntity();
		if (status == 204) {
			print("~");
			if (entity != null) {
				entity.consumeContent();
			}
		} else if (status != 405) {
			print("!");
			logger.error(resp.getStatusLine().getReasonPhrase() + " for "
					+ filename);
			if (entity != null) {
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					entity.writeTo(out);
					logger.debug(out.toString());
				} finally {
					entity.consumeContent();
				}
			}
		}
	}

	private void print(String string) {
		if (out != null) {
			out.print(string);
		}
	}

	private void println() {
		if (out != null) {
			out.println();
		}
	}

	private boolean isHidden(File file) {
		return file.isHidden() || file.getName().endsWith("~")
				|| file.getName().startsWith(".")
				|| file.getAbsolutePath().contains(File.separator + ".");
	}

	private boolean isWebResource(String path) {
		if (path == null || path.contains("/WEB-INF/")
				|| path.startsWith("WEB-INF/"))
			return false;
		return !isHidden(path);
	}

	private boolean isHidden(String path) {
		return path.endsWith("~") || path.startsWith(".")
				|| path.contains("/.") || path.contains("\\.");
	}

}
