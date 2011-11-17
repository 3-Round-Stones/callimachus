package org.callimachusproject.io;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class CarOutputStream implements Closeable, Flushable {
	private static final byte[] CAR_MAGIC_NUMBER;
	static {
		try {
			CAR_MAGIC_NUMBER = "CAR11RDF".getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
	private final ZipArchiveOutputStream zipStream;
	private final Set<String> closed = new HashSet<String>();

	public CarOutputStream(OutputStream out) throws IOException {
		out.write(CAR_MAGIC_NUMBER);
		this.zipStream = new ZipArchiveOutputStream(out);
		zipStream.setUseZip64(Zip64Mode.Always);
	}

	public OutputStream writeFolderEntry(String name, long time)
			throws IOException {
		if (name.charAt(name.length() - 1) != '/')
			throw new IllegalArgumentException("Folders must end with '/'");
		putArchiveEntry(name, time, null, MetaTypeExtraField.FOLDER);
		return openEntryStream();
	}

	public OutputStream writeResourceEntry(String name, long time, String type)
			throws IOException {
		putArchiveEntry(name, time, type, MetaTypeExtraField.RDF);
		return openEntryStream();
	}

	public OutputStream writeSchemaEntry(String name, long time, String type)
			throws IOException {
		putArchiveEntry(name, time, type, MetaTypeExtraField.RDFS);
		return openEntryStream();
	}

	public OutputStream writeFileEntry(String name, long time, String type)
			throws IOException {
		if (name.charAt(name.length() - 1) == '/')
			throw new IllegalArgumentException("Files cannot end with '/'");
		putArchiveEntry(name, time, type, MetaTypeExtraField.FILE);
		return openEntryStream();
	}

	@Override
	public void flush() throws IOException {
		zipStream.flush();
	}

	public void finish() throws IOException {
		zipStream.finish();
	}

	@Override
	public void close() throws IOException {
		zipStream.close();
	}

	private synchronized void putArchiveEntry(String name, long time, String type, MetaTypeExtraField mtype) throws IOException {
		if (!closed.add(name))
			throw new IllegalStateException(
					"Entry has already been added: " + name);
		ZipArchiveEntry entry = new ZipArchiveEntry(name);
		entry.setTime(time);
		entry.addExtraField(mtype);
		if (type != null) {
			entry.addExtraField(new ContentTypeExtraField(type));
		}
		zipStream.putArchiveEntry(entry);
	}

	private FilterOutputStream openEntryStream() {
		return new FilterOutputStream(zipStream) {
			private boolean closed;
			public void close() throws IOException {
				if (!closed) {
					closed = true;
					zipStream.closeArchiveEntry();
				}
			}
		};
	}

}
