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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import net.contentobjects.jnotify.JNotifyListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for changes to a directory and uploads or deletes the corresponding
 * files to a web server.
 * 
 * @author James Leigh
 * 
 */
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
			queue.clear();
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
