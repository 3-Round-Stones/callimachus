package org.callimachusproject.test;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.WebServer;
import org.callimachusproject.setup.CallimachusSetup;
import org.openrdf.repository.Repository;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;

public class TemporaryServerFactory {
	private static int MIN_PORT = 49152;
	private static int MAX_PORT = 65535;
	private static final String CHANGES_PATH = "../changes/";
	private static final String ERROR_XPL_PATH = "pipelines/error.xpl";
	private static final File WEBAPP_CAR = findCallimachusWebappCar();
	private static final int PORT = findPort(WEBAPP_CAR.getAbsolutePath().hashCode());
	private static final TemporaryServerFactory instance = new TemporaryServerFactory("http://localhost:" + PORT, PORT, "test@example.com", "test".toCharArray());

	public static TemporaryServerFactory getInstance() {
		return instance;
	}

	private static File findCallimachusWebappCar() {
		File dist = new File("dist");
		if (dist.list() != null) {
			for (String file : dist.list()) {
				if (file.startsWith("callimachus-webapp")
						&& file.endsWith(".car"))
					return new File(dist, file);
			}
		}
		throw new AssertionError("Could not find callimachus-webapp.car in "
				+ dist.getAbsolutePath());
	}

	private static int findPort(int seed) {
		int range = (MAX_PORT - MIN_PORT) / 2;
		return (seed % range) + range + MIN_PORT;
	}

	private final String origin;
	private final int port;
	private final String email;
	private final char[] password;
	private final Map<Integer, TemporaryServer> running = new LinkedHashMap<Integer, TemporaryServer>();

	public TemporaryServerFactory(String origin, int port, String email,
			char[] password) {
		assert email.indexOf('@') > 0;
		this.origin = origin;
		this.port = port;
		this.email = email;
		this.password = password;
	}

	public TemporaryServer createServer() {
		return new TemporaryServer() {
			private SoftReference<TemporaryServer> ref;

			private synchronized TemporaryServer getDelegate() {
				TemporaryServer delegate = null;
				if (ref != null) {
					delegate = ref.get();
				}
				if (delegate == null) {
					delegate = createTemporaryServer();
					ref = new SoftReference<TemporaryServer>(delegate);
				}
				return delegate;
			}

			public void start() throws InterruptedException, Exception {
				getDelegate().start();
			}

			public void pause() throws Exception {
				getDelegate().pause();
			}

			public void resume() throws Exception {
				getDelegate().resume();
			}

			public void stop() throws Exception {
				getDelegate().stop();
			}

			public void destroy() throws Exception {
				getDelegate().destroy();
			}

			public String getOrigin() {
				return getDelegate().getOrigin();
			}

			public String getUsername() {
				return getDelegate().getUsername();
			}

			public char[] getPassword() {
				return getDelegate().getPassword();
			}

			@Override
			public CalliRepository getRepository() {
				return getDelegate().getRepository();
			}
		};
	}

	private synchronized TemporaryServer createTemporaryServer() {
		try {
			final File dir = createCallimachus(origin);
			final LocalRepositoryManager manager = RepositoryProvider.getRepositoryManager(dir);
			final File dataDir = manager.getRepositoryDir("callimachus");
			final Repository repository = manager.getRepository("callimachus");
			return new TemporaryServer(){
				private final WebServer server = new WebServer(repository, dataDir);
				private boolean stopped;

				public synchronized void start() throws InterruptedException, Exception {
					server.setChangesPath(origin, CHANGES_PATH);
					server.setErrorPipe(origin, ERROR_XPL_PATH);
					server.addOrigin(origin);
					server.listen(new int[]{port}, new int[0]);
					server.start();
					Thread.sleep(100);
				}

				public synchronized void pause() throws Exception {
				}

				public synchronized void resume() throws Exception {
					synchronized (running) {
						TemporaryServer other = running.get(port);
						if (stopped || !this.equals(other)) {
							if (other != null) {
								other.stop();
							}
							start();
							running.put(port, this);
						}
					}
				}

				public synchronized void stop() throws Exception {
					if (!stopped) {
						server.stop();
						server.destroy();
						stopped = true;
						Thread.sleep(100);
					}
				}

				public synchronized void destroy() throws Exception {
					stop();
					manager.shutDown();
					FileUtil.deltree(dir);
				}

				@Override
				protected void finalize() throws Exception {
					destroy();
				}

				public String getOrigin() {
					return origin;
				}

				public String getUsername() {
					return email;
				}

				public char[] getPassword() {
					return password;
				}

				public CalliRepository getRepository() {
					return server.getRepository();
				}

				public String toString() {
					return server.toString();
				}
			};
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private File createCallimachus(String origin) throws Exception {
		String name = TemporaryServer.class.getSimpleName() + Integer.toHexString(WEBAPP_CAR.getAbsolutePath().hashCode());
		File dir = createDirectory(name);
		if (!dir.isDirectory() || WEBAPP_CAR.lastModified() > dir.lastModified()) {
			if (dir.isDirectory()) {
				FileUtil.deleteDir(dir);
			}
			dir.delete();
			String config = new Scanner(new File("etc", "callimachus-repository.ttl")).useDelimiter("\\A").next();
			System.setProperty("org.callimachusproject.config.webapp", WEBAPP_CAR.getAbsolutePath());
			CallimachusSetup setup = new CallimachusSetup(dir, config);
			setup.prepareWebappOrigin(origin);
			setup.createWebappOrigin(origin);
			setup.finalizeWebappOrigin(origin);
			String username = email.substring(0, email.indexOf('@'));
			setup.createAdmin(email, username, username, null, origin);
			setup.changeUserPassword(email, username, password, origin);
			setup.finalizeWebappOrigin(origin);
			setup.shutDown();
		}
		File temp = FileUtil.createTempDir(name);
		copyDir(dir, temp);
		return temp;
	}

	private void copyDir(File src, File dest) throws IOException {
		if (src.isDirectory()) {
			dest.mkdir();
			for (String file : src.list()) {
				copyDir(new File(src, file), new File(dest, file));
			}
		} else {
			FileUtil.copyFile(src, dest);
		}
	}

	private File createDirectory(String name) throws IOException {
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr != null) {
			File tmpDir = new File(tmpDirStr);
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
		}
		File tmp = File.createTempFile(name, "");
		tmp.delete();
		return new File(tmp.getParentFile(), name);
	}
}
