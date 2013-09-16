package org.callimachusproject.webdriver.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.callimachusproject.io.ChannelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetDownloader {
	private final Logger logger = LoggerFactory.getLogger(AssetDownloader.class);
	private final File dir;

	public AssetDownloader(File dir) {
		this.dir = dir;
	}

	public File download(String url) throws IOException,
			MalformedURLException, FileNotFoundException {
		File car = new File(dir, new File(URI.create(url)
				.getPath()).getName());
		if (!car.exists()) {
			logger.info("Downloading {}", url);
			InputStream in = new URL(url).openStream();
			try {
				File tmp = File.createTempFile("test", ".tmp", dir);
				try {
					OutputStream out = new FileOutputStream(tmp);
					try {
						ChannelUtil.transfer(in, out);
					} finally {
						out.close();
					}
					tmp.renameTo(car);
				} finally {
					if (tmp.exists()) {
						tmp.delete();
					}
				}
			} finally {
				in.close();
			}
		}
		return car;
	}
}
