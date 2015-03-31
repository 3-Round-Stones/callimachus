/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.webdriver.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;

import org.openrdf.http.object.io.ChannelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetDownloader {
	private final Logger logger = LoggerFactory
			.getLogger(AssetDownloader.class);
	private final File dir;
	private final String username;
	private final char[] password;

	public AssetDownloader(String username, char[] password) {
		this.dir = new File(System.getProperty("java.io.tmpdir"));
		this.username = username;
		this.password = password;
	}

	public AssetDownloader(File dir) {
		this.dir = dir;
		this.username = null;
		this.password = null;
	}

	public File getLocalAsset(String url) throws IOException {
		String filename = new File(URI.create(url).getPath()).getName();
		File car = new File(dir, filename);
		if (!car.exists()) {
			download(url, car);
		}
		return car;
	}

	public File downloadAsset(String url, String filename) throws IOException {
		File car = new File(dir, filename);
		download(url, car);
		return car;
	}

	private void download(String url, File file) throws IOException {
		dir.mkdirs();
		File tmp = File.createTempFile("test", ".part", dir);
		try {
			if (username != null && password != null) {
				Authenticator.setDefault(new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});
			}
			logger.info("Downloading {}", url);
			InputStream in = new URL(url).openStream();
			try {
				OutputStream out = new FileOutputStream(tmp);
				try {
					ChannelUtil.transfer(in, out);
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}
			if (file.exists()) {
				file.delete();
			}
			tmp.renameTo(file);
		} finally {
			Authenticator.setDefault(null);
			if (tmp.exists()) {
				tmp.delete();
			}
		}
	}
}
