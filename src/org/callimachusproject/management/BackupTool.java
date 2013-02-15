package org.callimachusproject.management;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupTool implements BackupToolMXBean {
	private static final ThreadFactory THREADFACTORY = new ThreadFactory() {
		public Thread newThread(Runnable r) {
			String name = BackupTool.class.getSimpleName() + "-"
					+ Integer.toHexString(r.hashCode());
			Thread t = new Thread(r, name);
			t.setDaemon(true);
			return t;
		}
	};

	private final Logger logger = LoggerFactory.getLogger(BackupTool.class);
	private final ExecutorService executor = Executors
			.newSingleThreadScheduledExecutor(THREADFACTORY);
	private Exception exception;
	private final File baseDir;
	private final File backupDir;
	private volatile boolean backup;
	private volatile boolean restore;

	public BackupTool(File baseDir, File backupDir) {
		this.baseDir = baseDir;
		this.backupDir = backupDir;
	}

	public String toString() {
		return backupDir.toString();
	}

	public synchronized void checkForErrors() throws IOException {
		try {
			if (exception != null)
				throw exception;
		} catch (RuntimeException e) {
			throw (RuntimeException) e;
		} catch (IOException e) {
			throw (IOException) e;
		} catch (Exception e) {
			throw new UndeclaredThrowableException(e);
		} finally {
			exception = null;
		}
	}

	public boolean isBackupInProgress() {
		return backup;
	}

	public boolean isRestoreInProgress() {
		return restore;
	}

	public void createBackup(final String label) throws Exception {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				blockCreateBackup(label);
				return null;
			}
		});
	}

	public String getBackupLabels() {
		File[] list = backupDir.listFiles();
		if (list == null)
			return "";
		Arrays.sort(list, new Comparator<File>() {
			public int compare(File o1, File o2) {
				if (o1.lastModified() < o2.lastModified())
					return 1;
				if (o1.lastModified() > o2.lastModified())
					return -1;
				return 0;
			}
		});
		StringBuilder result = new StringBuilder();
		for (File file : list) {
			String name = file.getName();
			if (name.endsWith(".zip") || name.endsWith(".ZIP")) {
				String label = name.substring(0, name.length() - 4);
				result.append(label).append(' ');
			}
		}
		if (result.length() == 0)
			return "";
		return result.substring(0, result.length() - 1);
	}

	public void restoreBackup(final String label) throws IOException {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				blockRestoreBackup(label);
				return null;
			}
		});
	}

	synchronized void blockCreateBackup(String label) throws IOException {
		backup = true;
		try {
			baseDir.mkdirs();
			String name = label.replaceAll("\\s+", "_") + ".zip";
			File backup = new File(backupDir, name);
			boolean replacing = backup.exists();
			boolean created = false;
			backup.getParentFile().mkdirs();
			FileOutputStream out = new FileOutputStream(backup);
			ZipOutputStream zos = new ZipOutputStream(out);
			try {
				String base = baseDir.getAbsolutePath() + "/";
				File repositories = new File(baseDir, "repositories");
				File[] listFiles = repositories.listFiles();
				if (listFiles != null) {
					for (File f : listFiles) {
						if (!"SYSTEM".equals(f.getName())) {
							if (!created && replacing) {
								logger.warn("Replacing {}", backup);
							} else if (!created) {
								logger.info("Creating {}", backup);
							}
							created |= putEntries(base, f, zos);
						}
					}
				}
				created |= putEntries(base, new File(baseDir, "www"), zos);
				created |= putEntries(base, new File(baseDir, "blob"), zos);
			} finally {
				if (created) {
					zos.close();
					logger.info("Created {}", backup);
				} else {
					out.close();
					backup.delete();
					if (replacing) {
						logger.warn("Deleted {}", backup);
					}
				}
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
			throw e;
		} finally {
			backup = false;
			notifyAll();
		}
	}

	synchronized void blockRestoreBackup(String label) throws IOException {
		restore = true;
		try {
			String name = label + ".zip";
			File backup = new File(backupDir, name);
			if (!backup.exists())
				throw new FileNotFoundException();
			logger.info("Restoring {}", backup);
			deleteAll(new File(baseDir, "repositories"));
			deleteAll(new File(baseDir, "www"));
			deleteAll(new File(baseDir, "blob"));
			FileInputStream fis = new FileInputStream(backup);
			ZipInputStream zis = new ZipInputStream(
					new BufferedInputStream(fis));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File file = new File(baseDir, entry.getName());
				file.getParentFile().mkdirs();
				FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream dest = new BufferedOutputStream(fos, 2048);
				try {
					int read;
					byte[] buf = new byte[2048];
					while ((read = zis.read(buf)) >= 0) {
						dest.write(buf, 0, read);
					}
				} finally {
					dest.close();
				}
			}
			zis.close();
			logger.info("Restored {}", backup);
		} catch (IOException e) {
			logger.error(e.toString(), e);
			throw e;
		} finally {
			restore = false;
			notifyAll();
		}
	}

	synchronized void saveError(Exception exc) {
		exception = exc;
	}

	synchronized void end() {
		notifyAll();
	}

	private Future<?> submit(final Callable<Void> task)
			throws IOException {
		checkForErrors();
		return executor.submit(new Runnable() {
			public void run() {
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

	private boolean putEntries(String base, File file, ZipOutputStream zos)
			throws IOException {
		if (file.isFile()) {
			String path = file.getAbsolutePath();
			String name = path;
			if (name.startsWith(base)) {
				name = name.substring(base.length());
			}
			ZipEntry entry = new ZipEntry(name);
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

	private void deleteAll(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				deleteAll(file);
			}
		}
		dir.delete();
	}
}
