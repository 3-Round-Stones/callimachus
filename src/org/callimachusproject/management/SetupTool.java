package org.callimachusproject.management;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.callimachusproject.setup.BlockingSetupTool;
import org.callimachusproject.setup.CallimachusConf;
import org.callimachusproject.setup.SetupOrigin;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupTool implements SetupToolMXBean {
	private static final ThreadFactory THREADFACTORY = new ThreadFactory() {
		public Thread newThread(Runnable r) {
			String name = SetupTool.class.getSimpleName() + "-"
					+ Integer.toHexString(r.hashCode());
			Thread t = new Thread(r, name);
			t.setDaemon(true);
			return t;
		}
	};

	private final Logger logger = LoggerFactory.getLogger(SetupTool.class);
	private final ExecutorService executor = Executors
			.newSingleThreadScheduledExecutor(THREADFACTORY);
	private final BlockingSetupTool blocking;
	private Exception exception;
	private int processing;

	public SetupTool(File baseDir, File rconfig, CallimachusConf conf)
			throws OpenRDFException {
		this.blocking = new BlockingSetupTool(baseDir, rconfig, conf);
	}

	public String toString() {
		return blocking.toString();
	}

	public LocalRepositoryManager getRepositoryManager() {
		return blocking.getRepositoryManager();
	}

	public File getRepositoryConfigFile() {
		return blocking.getRepositoryConfigFile();
	}

	public String getProperty(String key) throws IOException {
		return blocking.getProperty(key);
	}

	public void setProperty(String key, String value) throws IOException {
		blocking.setProperty(key, value);
	}

	public synchronized void checkForErrors() throws Exception {
		try {
			if (exception != null)
				throw exception;
		} finally {
			exception = null;
		}
	}

	public synchronized boolean isSetupInProgress() {
		return processing > 0;
	}

	public String[] getAvailableRepositoryTypes() throws IOException {
		return blocking.getAvailableRepositoryTypes();
	}

	public Map<String,String> getRepositoryProperties() throws IOException {
		Map<String, String> map = blocking.getRepositoryProperties();
		map = new LinkedHashMap<String, String>(map);
		Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext()) {
			if (iter.next().contains("password")) {
				iter.remove();
			}
		}
		return map;
	}

	public void setRepositoryProperties(Map<String,String> properties) throws IOException, OpenRDFException {
		try {
			blocking.setRepositoryProperties(properties);
		} catch (OpenRDFException e) {
			logger.error(e.toString(), e);
			throw new RepositoryConfigException(e.toString());
		}
	}

	public String getWebappOrigins() throws IOException {
		return blocking.getWebappOrigins();
	}

	public void setWebappOrigins(final String origins) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				latch.countDown();
				blocking.setWebappOrigins(origins);
				return null;
			}
		});
		latch.await();
	}

	public SetupOrigin[] getOrigins() throws IOException, OpenRDFException {
		return blocking.getOrigins();
	}

	public void addResolvableOrigin(final String origin, final String webappOrigin)
			throws Exception {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				blocking.addResolvableOrigin(origin, webappOrigin);
				return null;
			}
		});
	}

	public void addRootRealm(final String realm, final String webappOrigin)
			throws Exception {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				blocking.addRootRealm(realm, webappOrigin);
				return null;
			}
		});
	}

	public String[] getDigestEmailAddresses(String webappOrigin) throws OpenRDFException, IOException {
		return blocking.getDigestEmailAddresses(webappOrigin);
	}

	public void inviteAdminUser(final String email, final String username,
			final String label, final String comment, final String subject,
			final String body, final String webappOrigin) throws Exception {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				blocking.inviteAdminUser(email, username, label, comment, subject,
						body, webappOrigin);
				return null;
			}
		});
	}

	public boolean changeDigestUserPassword(String email, String password,
			String webappOrigin) throws OpenRDFException, IOException {
		return blocking.changeDigestUserPassword(email, password, webappOrigin);
	}

	synchronized void saveError(Exception exc) {
		exception = exc;
	}

	synchronized void begin() {
		processing++;
	}

	synchronized void end() {
		processing--;
		notifyAll();
	}

	protected Future<?> submit(final Callable<Void> task)
			throws Exception {
		checkForErrors();
		return executor.submit(new Runnable() {
			public void run() {
				begin();
				try {
					task.call();
				} catch (Exception exc) {
					saveError(exc);
				} finally {
					end();
				}
			}
		});
	}
}
