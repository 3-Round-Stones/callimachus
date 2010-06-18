package org.callimachusproject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.activation.MimeTypeParseException;
import javax.activation.MimetypesFileTypeMap;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.FileEntity;
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
	private static MimetypesFileTypeMap mimetypes = new MimetypesFileTypeMap();
	/** Date format pattern used to generate the header in RFC 1123 format. */
	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	/** The time zone to use in the date header. */
	public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static NamedThreadFactory threads = new NamedThreadFactory("Webaps Listener", true); 
	private Logger logger = LoggerFactory.getLogger(CallimachusServer.class);
	private final DateFormat dateformat;
	private int reloading;
	private int reloaded;
	private String authority;
	private String basic;
	private String authorization;
	private File webappsDir;
	private ObjectRepository repository;
	private HTTPObjectServer server;
	private String origin;
	private WebAppListener listener;

	public CallimachusServer(Repository repository, File dataDir, File webapps)
			throws Exception {
		dateformat = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
		dateformat.setTimeZone(GMT);
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
		basic = "boot:" + generatePassword();
		webappsDir = webapps.getCanonicalFile();
		this.repository = importJars(webappsDir, repository);
		server = createServer(dataDir, basic, this.repository);
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
		server.setIdentityPrefix("http://" + authority + IDENTITY_PATH);
	}

	public int getPort() {
		return server.getPort();
	}

	public void setPort(int port) {
		server.setPort(port);
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

	public void start() throws Exception {
		boolean live = repository.isCompileRepository();
		if (authority == null) {
			setAuthority(getAuthority(getPort()));
		}
		server.start();
		Thread.sleep(1000);
		if (server.isRunning()) {
			origin = "http://" + authority + "/";
			logger.info("Callimachus is listening at {}", origin);
			byte[] decoded = basic.getBytes();
			String cred = new String(Base64.encodeBase64(decoded), "8859_1");
			authorization = "Basic " + cred;
			InetSocketAddress host;
			if (authority.contains(":")) {
				int idx = authority.indexOf(':');
				int port = Integer.parseInt(authority.substring(idx + 1));
				host = new InetSocketAddress(authority.substring(0, idx), port);
			} else {
				host = new InetSocketAddress(authority, 80);
			}
			HTTPObjectClient.getInstance().setProxy(host, server);
			starting(server.getPort(), authorization);
			boolean empty = isEmpty(repository);
			if (live && empty) {
				repository.setCompileRepository(false);
			}
			System.out.print("Uploading Webapps");
			try {
				int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED
						| JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;
				listener = new WebAppListener();
				try {
					JNotify.addWatch(webappsDir.getCanonicalPath(), mask, true,
							listener);
					Thread listenerThread = threads.newThread(listener);
					listenerThread.start();
				} catch (NoClassDefFoundError e) {
					logger.error(e.getMessage());
				} catch (UnsatisfiedLinkError e) {
					logger.error(e.getMessage());
				}
				uploadWebApps(webappsDir, server.getPort(), origin, "",
						authorization, !empty);
				if (live) {
					repository.setCompileRepository(true);
				}
				started(server.getPort(), authorization);
			} finally {
				System.out.println();
			}
		}
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public void stop() throws Exception {
		if (listener != null) {
			listener.stop();
		}
		stopping(server.getPort(), authorization);
		server.stop();
	}

	private final class CharsetDetector implements nsICharsetDetectionObserver {
		private Charset charset;

		public Charset detect(File file, boolean gzip) throws IOException {
			boolean ascii = true;
			nsDetector det = new nsDetector();
			det.Init(this);
			InputStream in = new FileInputStream(file);
			if (gzip) {
				in = new GZIPInputStream(in);
			}
			try {
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
			} finally {
				in.close();
			}
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
			queue.add(eos);
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

		public void fileModified(int wd, String rootPath,
				String name) {
			if (name.contains("/WEB-INF/"))
				return;
			File file = new File(new File(rootPath), name).getAbsoluteFile();
			if (!isHidden(file)) {
				add(file);
			}
		}

		@Override
		public void run() {
			File file;
			while ((file = take()) != null) {
				try {
					int port = server.getPort();
					String rootPath = webappsDir.getCanonicalPath() + File.separator;
					String path = file.getAbsolutePath();
					if (path.startsWith(rootPath)) {
						path = path.substring(rootPath.length());
					}
					path = getPath(path);
					if (file.exists()) {
						reloading(port, authorization);
						uploadWebApps(file, port, origin, path, authorization,
								true);
						reloaded(port, authorization);
					} else {
						reloading(port, authorization);
						deleteFile(port, authorization, origin, path, file);
						reloaded(port, authorization);
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
				queue.notify();
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

	private boolean isEmpty(ObjectRepository repository) throws RepositoryException {
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
			throws MalformedURLException, URISyntaxException,
			RepositoryConfigException, RepositoryException {
		List<File> jars = findJars(webappsDir, false, false, false,
				new ArrayList<File>());
		ObjectRepository or;
		if (jars.isEmpty() && repository instanceof ObjectRepository) {
			or = (ObjectRepository) repository;
		} else {
			ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
			ObjectRepositoryConfig config = factory.getConfig();
			config.setCompileRepository(true);
			if (!jars.isEmpty()) {
				for (File jar : jars) {
					if (jar.isDirectory() || jar.getName().endsWith(".jar")
							|| jar.getName().endsWith(".JAR")) {
						config.addConceptJar(jar.toURI().toURL());
					} else {
						config.addImports(jar.toURI().toURL());
					}
				}
			}
			or = factory.createRepository(config, repository);
		}
		return or;
	}

	private List<File> findJars(File dir, boolean web, boolean lib,
			boolean ont, ArrayList<File> jars) {
		if (isHidden(dir))
			return jars;
		String name = dir.getName();
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				boolean webinf = name.equalsIgnoreCase("WEB-INF");
				boolean eqlib = name.equalsIgnoreCase("lib");
				boolean ontologies = name.equalsIgnoreCase("ontologies");
				findJars(file, webinf, lib || web && eqlib, ont || web
						&& ontologies, jars);
			}
			if (web && name.equalsIgnoreCase("classes")) {
				jars.add(dir);
			}
		} else if (lib && (name.endsWith(".jar") || name.endsWith(".JAR"))) {
			jars.add(dir);
		} else if (ont) {
			jars.add(dir);
		}
		return jars;
	}

	private void starting(int port, String authorization)
			throws GatewayTimeout, IOException, InterruptedException {
		InetSocketAddress server = new InetSocketAddress(InetAddress
				.getLocalHost(), port);
		HttpRequest req = new BasicHttpRequest("POST", NS + "boot?starting");
		req.setHeader("Authorization", authorization);
		HttpResponse resp = HTTPObjectClient.getInstance().service(server, req);
		StatusLine status = resp.getStatusLine();
		if (status.getStatusCode() != 204) {
			logger.error(status.getReasonPhrase() + " while starting");
		}
	}

	private void started(int port, String authorization) throws GatewayTimeout,
			IOException, InterruptedException {
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

	private synchronized void reloading(int port, String authorization)
			throws GatewayTimeout, IOException, InterruptedException {
		if (reloading++ == reloaded) {
			InetSocketAddress server = new InetSocketAddress(InetAddress
					.getLocalHost(), port);
			HttpRequest req = new BasicHttpRequest("POST", NS + "boot?reloading");
			req.setHeader("Authorization", authorization);
			HttpResponse resp = HTTPObjectClient.getInstance().service(server,
					req);
			StatusLine status = resp.getStatusLine();
			if (status.getStatusCode() != 204) {
				logger.error(status.getReasonPhrase() + " while reloading");
			}
		}
	}

	private synchronized void reloaded(final int port,
			final String authorization) throws GatewayTimeout, IOException,
			InterruptedException {
		final int loaded = reloading;
		executor.schedule(new Runnable() {
			public void run() {
				try {
					if (reloading == loaded) {
						reloaded = loaded;
						InetSocketAddress server = new InetSocketAddress(
								InetAddress.getLocalHost(), port);
						HttpRequest req = new BasicHttpRequest("POST", NS
								+ "boot?reloaded");
						req.setHeader("Authorization", authorization);
						HttpResponse resp = HTTPObjectClient.getInstance()
								.service(server, req);
						StatusLine status = resp.getStatusLine();
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

	private void stopping(int port, String authorization)
			throws GatewayTimeout, IOException, InterruptedException {
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

	private void uploadWebApps(File file, int port, String origin, String path,
			String authorization, boolean update) throws InterruptedException,
			ExecutionException, IOException {
		String name = file.getName();
		if (isHidden(file) || "WEB-INF".equals(name))
			return;
		boolean gzip = name.endsWith(".gz");
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				uploadWebApps(f, port, origin, getPath(path, f.getName()),
						authorization, update);
			}
		} else {
			HttpResponse resp = upload(file, port, authorization, origin, path,
					gzip, update);
			int status = resp.getStatusLine().getStatusCode();
			HttpEntity entity = resp.getEntity();
			if (status == 412) {
				System.out.print(".");
				if (entity != null) {
					entity.consumeContent();
				}
			} else if (status == 204) {
				System.out.print("^");
				if (entity != null) {
					entity.consumeContent();
				}
			} else {
				System.out.print("!");
				logger.error(resp.getStatusLine().getReasonPhrase() + " for "
						+ file.getName());
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
	}

	private String getPath(String path) {
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

	private HttpResponse upload(File file, int port, String authorization,
			String origin, String path, boolean gzip, boolean update) throws IOException,
			InterruptedException, ExecutionException {
		long size = file.length();
		String type = getContentType(file, gzip);
		if (type.startsWith("text/") && !type.contains("charset")) {
			type += ";charset=" + detectCharset(file, gzip).name();
		}
		String since = dateformat.format(new Date(file.lastModified()));
		BasicHttpEntityEnclosingRequest req;
		req = new BasicHttpEntityEnclosingRequest("PUT", origin + path);
		req.setHeader("Authorization", authorization);
		if (update) {
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
		return resp;
	}

	private String getContentType(File file, boolean gzip) {
		String name = file.getName();
		if (gzip) {
			name = name.replaceAll(".gz$", "");
		}
		return mimetypes.getContentType(name);
	}

	private Charset detectCharset(File file, boolean gzip) throws IOException {
		return new CharsetDetector().detect(file, gzip);
	}

	private void deleteFile(int port, String authorization, String origin,
			String path, File file) throws IOException, InterruptedException,
			ExecutionException {
		BasicHttpRequest req = new BasicHttpRequest("DELETE", origin + path);
		req.setHeader("Authorization", authorization);
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.service(req);
		int status = resp.getStatusLine().getStatusCode();
		HttpEntity entity = resp.getEntity();
		if (status == 204) {
			System.out.print("~");
			if (entity != null) {
				entity.consumeContent();
			}
		} else if (status != 405) {
			System.out.print("!");
			logger.error(resp.getStatusLine().getReasonPhrase() + " for "
					+ file.getName());
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

	private boolean isHidden(File file) {
		return file.isHidden() || file.getName().endsWith("~")
				|| file.getName().startsWith(".")
				|| file.getAbsolutePath().contains(File.separator + ".");
	}

}
