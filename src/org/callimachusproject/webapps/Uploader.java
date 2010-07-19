package org.callimachusproject.webapps;

import static java.util.Collections.singleton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.activation.MimetypesFileTypeMap;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.http.object.util.NamedThreadFactory;
import org.openrdf.http.object.util.SharedExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Uploader {

	public static boolean isHidden(File file) {
		return file.isHidden() || file.getName().endsWith("~")
				|| file.getName().startsWith(".")
				|| file.getAbsolutePath().contains(File.separator + ".");
	}

	public static boolean isHidden(String path) {
		return path.endsWith("~") || path.startsWith(".")
				|| path.contains("/.") || path.contains("\\.");
	}

	private static final String NS = "http://callimachusproject.org/rdf/2009/framework#";
	private static ScheduledExecutorService executor = SharedExecutors
			.getTimeoutThreadPool();
	private static NamedThreadFactory threads = new NamedThreadFactory(
			"Webaps Listener", true);
	/** Date format pattern used to generate the header in RFC 1123 format. */
	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	/** The time zone to use in the date header. */
	public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private Logger logger = LoggerFactory.getLogger(Uploader.class);
	private final DateFormat dateformat;
	private WebAppListener listener;
	private MultiValuedFileMap entries;
	private MultiValuedFileMap origins;
	private MimetypesFileTypeMap mimetypes;
	private InetSocketAddress proxy;
	private int reloading;
	private PrintStream out;
	private File webappsDir;
	private String authorization;
	private String origin;

	public Uploader(MimetypesFileTypeMap mimetypes, File dataDir) throws IOException {
		this.mimetypes = mimetypes;
		File entriesDir = new File(dataDir, "entries").getCanonicalFile();
		entries = new MultiValuedFileMap(entriesDir);
		entries = new MultiValuedFileMap(entriesDir);
		File originsDir = new File(dataDir, "origins").getCanonicalFile();
		origins = new MultiValuedFileMap(originsDir);
		dateformat = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
		dateformat.setTimeZone(GMT);
	}

	public File getWebappsDir() {
		return webappsDir;
	}

	public void setWebappsDir(File webappsDir) throws IOException {
		this.webappsDir = webappsDir.getCanonicalFile();
	}

	public void setAuthorization(String authorization) {
		this.authorization = authorization;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public void setProxy(InetSocketAddress proxy) {
		this.proxy = proxy;
	}

	public void stop() {
		if (listener != null) {
			listener.stop();
		}
	}

	public void printStatus(PrintStream out) {
		this.out = out;
	}

	public void uploadWebapps(boolean conditional) throws IOException,
			JNotifyException, InterruptedException {
		print("Uploading Webapps");
		try {
			Thread listenerThread = null;
			int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED
					| JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;
			try {
				WebAppListener wal = new WebAppListener(this);
				JNotify.addWatch(webappsDir.getPath(), mask, true, wal);
				listenerThread = threads.newThread(wal);
				listener = wal;
			} catch (NoClassDefFoundError e) {
				logger.error(e.getMessage());
			} catch (UnsatisfiedLinkError e) {
				logger.error(e.getMessage());
			}
			conditional = conditional && !deleteMissingFiles();
			origins.put(webappsDir, singleton(origin));
			uploadWebApps(webappsDir, "", conditional);
			if (listener != null && listenerThread != null) {
				listenerThread.start();
				listener.await();
			}
		} finally {
			println();
		}
	}

	public void uploadWebApps(File file, boolean conditional)
			throws IOException {
		uploadWebApps(file, getWebPath(file), conditional);
	}

	public void deleteFile(File file) throws IOException {
		file = file.getAbsoluteFile();
		String path = getWebPath(file);
		String origin = getOriginFor(file);
		deleteResource(origin + path, file.getName());
		if (isWar(path)) {
			String wp = path.substring(0, path.length() - 4);
			for (String entry : entries.get(file)) {
				deleteWarEntry(file, origin + wp, entry);
			}
			entries.remove(file);
		}
	}

	public void started() throws GatewayTimeout, IOException,
			InterruptedException {
		HttpRequest req = new BasicHttpRequest("POST", NS + "boot?started");
		req.setHeader("Authorization", authorization);
		HttpResponse resp = HTTPObjectClient.getInstance().service(proxy, req);
		StatusLine status = resp.getStatusLine();
		if (status.getStatusCode() != 204) {
			logger.error(status.getReasonPhrase() + " once started");
		}
	}

	public void stopping() throws GatewayTimeout, IOException,
			InterruptedException {
		try {
			HttpRequest req = new BasicHttpRequest("POST", NS + "boot?stopping");
			req.setHeader("Authorization", authorization);
			HttpResponse resp = HTTPObjectClient.getInstance().service(proxy,
					req);
			StatusLine status = resp.getStatusLine();
			if (status.getStatusCode() != 204) {
				logger.error(status.getReasonPhrase() + " while stopping");
			}
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
	}

	public synchronized void reloading() throws GatewayTimeout, IOException,
			InterruptedException {
		reloading++;
	}

	public synchronized void reloaded() throws GatewayTimeout, IOException,
			InterruptedException {
		final int loaded = reloading;
		executor.schedule(new Runnable() {
			public void run() {
				try {
					if (reloading == loaded) {
						HttpRequest req = new BasicHttpRequest("POST", NS
								+ "boot?reloaded");
						req.setHeader("Authorization", authorization);
						HttpResponse resp = HTTPObjectClient.getInstance()
								.service(proxy, req);
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

	private File getWebAppsDirFor(File file) {
		return webappsDir;
	}

	private String getOriginFor(File file) {
		return origin;
	}

	private boolean isWebappDir(File webapps) throws IOException {
		return webappsDir.getCanonicalFile().equals(webapps.getCanonicalFile());
	}

	private boolean isWar(String path) {
		return path.endsWith(".war") || path.endsWith(".WAR");
	}

	private String getWebPath(File file) throws IOException {
		File webAppsDir = getWebAppsDirFor(file);
		return getWebPath(webAppsDir, file);
	}

	private String getWebPath(File webAppsDir, File file) {
		String rootPath = webAppsDir.getPath() + File.separator;
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

	private void uploadWebApps(File file, String path, boolean conditional)
			throws IOException {
		String name = file.getName();
		if (isHidden(file) || "WEB-INF".equals(name) || "META-INF".equals(name))
			return;
		boolean gzip = name.endsWith(".gz");
		file = file.getAbsoluteFile();
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				uploadWebApps(f, getPath(path, f.getName()), conditional);
			}
		} else if (isWar(file.getName())) {
			if (isWar(path)) {
				path = path.substring(0, path.length() - 4);
			}
			uploadWarFile(file, path, conditional);
		} else {
			HttpResponse resp = upload(file, path, gzip, conditional);
			entries.add(getWebAppsDirFor(file), file.getAbsolutePath());
			report(resp, path, file.getName());
		}
	}

	private boolean deleteMissingFiles() throws IOException {
		boolean modified = false;
		for (File webapps : entries.keySet()) {
			if (isWar(webapps.getName())) {
				File file = webapps;
				File webAppsDir = getWebAppsDirFor(file);
				if (file.getPath().startsWith(webAppsDir.getPath())) {
					Set<String> origin = singleton(getOriginFor(file));
					if (origins.get(webAppsDir).equals(origin)) {
						if (file.exists())
							continue;
					}
				}
				for (String origin : origins.get(webAppsDir)) {
					String path = getWebPath(file);
					String wp = path.substring(0, path.length() - 4);
					for (String entry : entries.get(file)) {
						modified = true;
						deleteWarEntry(file, origin + wp, entry);
					}
				}
			} else {
				for (String entry : entries.get(webapps)) {
					File file = new File(entry);
					if (isWebappDir(webapps)) {
						File webAppsDir = getWebAppsDirFor(file);
						Set<String> origin = singleton(getOriginFor(file));
						if (origins.get(webAppsDir).equals(origin)) {
							if (file.exists())
								continue;
						}
					}
					for (File webAppsDir : origins.keySet()) {
						if (file.getPath().startsWith(webAppsDir.getPath())) {
							String path = getWebPath(webAppsDir, file);
							for (String origin : origins.get(webAppsDir)) {
								modified = true;
								deleteResource(origin + path, file.getName());
							}
						}
					}
				}
			}
			entries.remove(webapps);
		}
		return modified;
	}

	private HttpResponse upload(File file, String path, boolean gzip,
			boolean conditional) throws IOException {
		long size = file.length();
		String type = getContentType(file.getName(), gzip);
		if (type.startsWith("text/") && !type.contains("charset")) {
			type += ";charset=" + detectCharset(file, gzip).name();
		}
		long modified = file.lastModified();
		String since = dateformat.format(new Date(modified));
		BasicHttpEntityEnclosingRequest req;
		req = new BasicHttpEntityEnclosingRequest("PUT", getOriginFor(file) + path);
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
		HttpResponse resp = client.service(proxy, req);
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
			throws ZipException, IOException {
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
			for (String entry : this.entries.get(file)) {
				if (!names.contains(entry)) {
					deleteWarEntry(file, getOriginFor(file) + path, entry);
				}
			}
			this.entries.put(file, names);
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
		String origin = getOriginFor(new File(zip.getName()));
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
			return HTTPObjectClient.getInstance().service(proxy, req);
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

	private String getContentType(String filename, boolean gzip) {
		if (gzip) {
			filename = filename.replaceAll(".gz$", "");
		}
		return mimetypes.getContentType(filename);
	}

	private Charset detectCharset(File file, boolean gzip) throws IOException {
		return new CharsetDetector().detect(file, gzip);
	}

	private void deleteWarEntry(File file, String url, String entry)
			throws IOException {
		String p = entry.replace('\\', '/');
		if (p.length() > 0 && !p.endsWith("/")) {
			String ep = url;
			String name = entry;
			int idx = p.lastIndexOf('/');
			if (idx >= 0) {
				ep = ep + "/" + p.substring(0, idx);
				name = p.substring(idx + 1);
			}
			deleteResource(getPath(ep, name), file.getName() + "!" + entry);
		}
	}

	private boolean isWebResource(String path) {
		if (path == null)
			return false;
		if (path.contains("/WEB-INF/") || path.startsWith("WEB-INF/")
				|| path.contains("/META-INF/") || path.startsWith("META-INF/"))
			return false;
		return !isHidden(path);
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

	private void deleteResource(String url, String filename) throws IOException {
		BasicHttpRequest req = new BasicHttpRequest("DELETE", url);
		req.setHeader("Authorization", authorization);
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.service(proxy, req);
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
}
