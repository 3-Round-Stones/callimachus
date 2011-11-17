package org.callimachusproject.io;

import java.io.UnsupportedEncodingException;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipShort;

class ContentTypeExtraField implements ZipExtraField {
	private static final String[] PREFIX = { "application/", "audio/",
			"example/", "image/", "message/", "model/", "multipart/", "text/",
			"video/" };
	private static final ZipShort ID = new ZipShort(0x67D5);

	static String parseExtraField(ZipArchiveEntry entry) {
		try {
			ZipExtraField field = entry.getExtraField(ID);
			if (field == null)
				return null;
			byte[] data = field.getLocalFileDataData();
			String str = new String(data, "UTF-8");
			if (isContentType(str))
				return str;
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
		return null;
	}

	private static boolean isContentType(String str) {
		for (int i = 0; i < PREFIX.length; i++) {
			if (str.startsWith(PREFIX[i]))
				return true;
		}
		return false;
	}

	private final byte[] data;

	ContentTypeExtraField(String type) {
		if (!isContentType(type))
			throw new IllegalArgumentException("Not a valid MIME Media Type: "
					+ type);
		try {
			this.data = type.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	public ZipShort getHeaderId() {
		return ID;
	}

	public byte[] getLocalFileDataData() {
		return data;
	}

	public ZipShort getLocalFileDataLength() {
		return new ZipShort(data.length);
	}

	public void parseFromLocalFileData(byte[] buffer, int offset, int length)
			throws ZipException {
		throw new UnsupportedOperationException();
	}

	public byte[] getCentralDirectoryData() {
		return getLocalFileDataData();
	}

	public ZipShort getCentralDirectoryLength() {
		return getLocalFileDataLength();
	}

	public void parseFromCentralDirectoryData(byte[] buffer, int offset,
			int length) throws ZipException {
		parseFromLocalFileData(buffer, offset, length);
	}

}