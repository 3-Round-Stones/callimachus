package org.callimachusproject.webapps;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import net.contentobjects.jnotify.JNotifyListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppListener implements JNotifyListener, Runnable {
	private Logger logger = LoggerFactory.getLogger(WebAppListener.class);
	private final Uploader uploader;
	private Queue<File> queue = new LinkedList<File>();
	private File eos, wait;

	public WebAppListener(Uploader uploader) throws IOException {
		this.uploader = uploader;
		eos = File.createTempFile("callimachus", "eos");
		wait = File.createTempFile("callimachus", "wait");
		eos.delete();
		wait.delete();
	}

	public String toString() {
		return "watch for changes to " + uploader;
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
		if (name == null || name.contains("/WEB-INF/") || name.contains("/META-INF/"))
			return;
		File file = new File(new File(rootPath), name).getAbsoluteFile();
		if (!Uploader.isHidden(file)) {
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
				if (file.exists()) {
					this.uploader.reloading();
					this.uploader.uploadWebApps(file, false);
					this.uploader.reloaded();
				} else {
					this.uploader.reloading();
					this.uploader.deleteFile(file);
					this.uploader.reloaded();
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