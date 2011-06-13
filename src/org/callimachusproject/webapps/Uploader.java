/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.webapps;

import static java.util.Collections.singleton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
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
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.http.object.util.ManagedExecutors;
import org.openrdf.repository.object.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uploads all the files in a given directory to a webserver using the PUT method.
 * 
 * @author James Leigh
 *
 */
public class Uploader {

	private static final String SUFFIX = "?calliwebapps";

	public static boolean isHidden(File file) {
		return file.isHidden() || file.getName().endsWith("~")
				|| file.getName().startsWith(".")
				|| file.getAbsolutePath().contains(File.separator + ".");
	}

	public static boolean isHidden(String path) {
		return path.endsWith("~") || path.startsWith(".")
				|| path.contains("/.") || path.contains("\\.");
	}

	private static ScheduledExecutorService executor = ManagedExecutors
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
	private File webappsDir;
	private String authorization;
	private String origin;
	private List<UploadListener> listeners = new ArrayList<UploadListener>();

	public Uploader(MimetypesFileTypeMap mimetypes, File dataDir) throws IOException {
		this.mimetypes = mimetypes;
		File entriesDir = new File(dataDir, "entries").getCanonicalFile();
		entries = new MultiValuedFileMap(entriesDir, "entries.list", true);
		File originsDir = new File(dataDir, "origins").getCanonicalFile();
		origins = new MultiValuedFileMap(originsDir, "entries.list", true);
		dateformat = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
		dateformat.setTimeZone(GMT);
		// import 0.10 format
		entries.addAll(new MultiValuedFileMap(entriesDir, "keys.list", false));
		origins.addAll(new MultiValuedFileMap(originsDir, "keys.list", false));
	}

	public String toString() {
		if (webappsDir != null)
			return webappsDir.toString();
		return super.toString();
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

	public void addListener(UploadListener listener) {
		listeners.add(listener);
	}

	public void removeListener(UploadListener listener) {
		listeners.remove(listener);
	}

	public void stop() {
		if (listener != null) {
			listener.stop();
		}
	}

	public void uploadWebapps(boolean conditional) throws IOException,
			JNotifyException, InterruptedException {
		notifyStarting();
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
		notifyStarted();
	}

	public void stopping() throws GatewayTimeout, IOException,
			InterruptedException {
		notifyStopping();
	}

	public synchronized void reloading() throws GatewayTimeout, IOException,
			InterruptedException {
		reloading++;
		notifyReloading();
	}

	public synchronized void reloaded() throws GatewayTimeout, IOException,
			InterruptedException {
		final int loaded = reloading;
		executor.schedule(new Runnable() {
			public String toString() {
				return "fire reload event";
			}

			public void run() {
				try {
					if (reloading == loaded) {
						notifyReloaded();
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
			String url = getOriginFor(file) + path;
			HttpResponse resp = upload(file, url, gzip, conditional);
			entries.add(getWebAppsDirFor(file), file.getAbsolutePath());
			report(resp, url, file.getName());
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

	private HttpResponse upload(File file, String url, boolean gzip,
			boolean conditional) throws IOException {
		long size = file.length();
		String type = getContentType(file.getName(), gzip);
		if (type.startsWith("text/") && !type.contains("charset")) {
			type += ";charset=" + detectCharset(file, gzip).name();
		}
		long modified = file.lastModified();
		String since = dateformat.format(new Date(modified));
		BasicHttpEntityEnclosingRequest req;
		notifyUploading(url, type);
		req = new BasicHttpEntityEnclosingRequest("PUT", url + SUFFIX);
		req.setHeader("Authorization", authorization);
		if (conditional) {
			req.setHeader("If-Unmodified-Since", since);
		}
		req.setHeader("Content-Type", type);
		req.setHeader("Content-Length", Long.toString(size));
		if (gzip) {
			req.setHeader("Content-Encoding", "gzip");
			logger.debug("{}\tcompressed {}", url, type);
		} else {
			logger.debug("{}\t{}", url, type);
		}
		req.setEntity(new FileEntity(file, type));
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.service(proxy, req);
		if (size != file.length() || modified != file.lastModified()) {
			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
			return upload(file, url, gzip, false);
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
				String origin = getOriginFor(new File(zip.getName()));
				String url = origin + dest;
				HttpResponse resp = uploadEntry(zip, ze, url, conditional);
				report(resp, url, file.getName() + "!" + entry);
			}
		} finally {
			zip.close();
		}
	}

	private HttpResponse uploadEntry(ZipFile zip, ZipEntry entry, String url,
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
		notifyUploading(url, type);
		req = new BasicHttpEntityEnclosingRequest("PUT", url + SUFFIX);
		req.setHeader("Authorization", authorization);
		if (conditional) {
			req.setHeader("If-Unmodified-Since", since);
		}
		req.setHeader("Content-Type", type);
		req.setHeader("Content-Length", Long.toString(size));
		logger.debug("{}\t{}", url, type);
		InputStream in = zip.getInputStream(entry);
		req.setEntity(new InputStreamEntity(in, entry.getSize()));
		try {
			return HTTPObjectClient.getInstance().service(proxy, req);
		} finally {
			in.close();
		}
	}

	private void report(HttpResponse resp, String url, String filename)
			throws IOException {
		StatusLine line = resp.getStatusLine();
		int status = line.getStatusCode();
		HttpEntity entity = resp.getEntity();
		if (status == 412) {
			notifyNotModified(url);
			if (entity != null) {
				entity.consumeContent();
			}
		} else if (status == 204) {
			notifyUploaded(url);
			if (entity != null) {
				entity.consumeContent();
			}
		} else {
			notifyError(url, status, line.getReasonPhrase());
			logger.error(line.getReasonPhrase() + " for "
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
		if (directory.length() == 0)
			return name.replace(' ', '+');
		return directory + "/" + name.replace(' ', '+');
	}

	private void deleteResource(String url, String filename) throws IOException {
		notifyRemoving(url);
		BasicHttpRequest req = new BasicHttpRequest("DELETE", url + SUFFIX);
		req.setHeader("Authorization", authorization);
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.service(proxy, req);
		StatusLine line = resp.getStatusLine();
		int status = line.getStatusCode();
		HttpEntity entity = resp.getEntity();
		if (status == 204) {
			notifyRemoved(url);
			if (entity != null) {
				entity.consumeContent();
			}
		} else if (status != 405) {
			notifyError(url, line.getStatusCode(), line.getReasonPhrase());
			logger.error(line.getReasonPhrase() + " for "
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

	private void notifyStarting() {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyStarting();
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyStarted() {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyStarted();
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyReloading() {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyReloading();
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyReloaded() {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyReloaded();
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyStopping() {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyStopping();
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyUploading(String url, String type) {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyUploading(url, type);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyUploaded(String url) {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyUploaded();
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyNotModified(String url) {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyNotModified(url);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyRemoving(String url) {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyRemoving(url);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyRemoved(String url) {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyRemoved(url);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void notifyError(String url, int code, String reason) {
		for (UploadListener listener : listeners) {
			try {
				listener.notifyError(url, code, reason);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}
}
