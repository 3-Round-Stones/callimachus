package org.callimachusproject.io;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public class CarInputStream implements Closeable {
	private static final byte[] CAR_MAGIC_NUMBER;
	static {
		try {
			CAR_MAGIC_NUMBER = "CAR11RDF".getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
	private final ZipArchiveInputStream zipStream;
	private ZipArchiveEntry entry;

	public CarInputStream(InputStream in) throws IOException {
		byte[] magic = new byte[CAR_MAGIC_NUMBER.length];
		int read = in.read(magic);
		while (read < magic.length) {
			read += in.read(magic, read, magic.length - read);
		}
		if (!Arrays.equals(CAR_MAGIC_NUMBER, magic))
			throw new IOException("InputStream not in CAR format");
		this.zipStream = new ZipArchiveInputStream(in);
	}

	public void close() throws IOException {
		zipStream.close();
	}

	public synchronized String readEntryName() throws IOException {
		if (entry == null) {
			entry = zipStream.getNextZipEntry();
		}
		if (entry == null)
			return null;
		return entry.getName();
	}

	public synchronized long getEntryTime() throws IOException {
		if (entry == null)
			return -1;
		return entry.getTime();
	}

	public synchronized String getEntryType() throws IOException {
		if (entry == null)
			return null;
		return ContentTypeExtraField.parseExtraField(entry);
	}

	public synchronized boolean isFolderEntry() throws IOException {
		if (entry == null)
			return false;
		return MetaTypeExtraField.FOLDER == MetaTypeExtraField.parseExtraField(entry);
	}

	public synchronized boolean isResourceEntry() throws IOException {
		if (entry == null)
			return false;
		return MetaTypeExtraField.RDF == MetaTypeExtraField.parseExtraField(entry);
	}

	public synchronized boolean isSchemaEntry() throws IOException {
		if (entry == null)
			return false;
		return MetaTypeExtraField.RDFS == MetaTypeExtraField.parseExtraField(entry);
	}

	public synchronized boolean isFileEntry() throws IOException {
		if (entry == null)
			return false;
		return MetaTypeExtraField.FILE == MetaTypeExtraField.parseExtraField(entry);
	}

	public synchronized InputStream readEntryStream() throws IOException {
		if (entry == null)
			throw new IllegalStateException(
					"readEntryName must be called before readEntryStream");
		final ZipArchiveEntry openEntry = entry;
		return new FilterInputStream(zipStream) {
			public void close() throws IOException {
				if (openEntry == entry) {
					entry = null;
				}
			}
		};
	}

}
