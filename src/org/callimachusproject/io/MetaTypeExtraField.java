package org.callimachusproject.io;

import java.io.UnsupportedEncodingException;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipShort;

class MetaTypeExtraField implements ZipExtraField {
	static final MetaTypeExtraField FILE = new MetaTypeExtraField("FILE");
	static final MetaTypeExtraField FOLDER = new MetaTypeExtraField("FOLDER");
	static final MetaTypeExtraField RDF = new MetaTypeExtraField("RDF");
	static final MetaTypeExtraField RDFS = new MetaTypeExtraField("RDFS");
	static MetaTypeExtraField parseExtraField(ZipArchiveEntry entry) {
		try {
			ZipExtraField field = entry.getExtraField(ID);
			if (field == null)
				return null;
			byte[] data = field.getLocalFileDataData();
			String str = new String(data, "UTF-8");
			if ("FILE".equals(str))
				return FILE;
			if ("FOLDER".equals(str))
				return FOLDER;
			if ("RDF".equals(str))
				return RDF;
			if ("RDFS".equals(str))
				return RDFS;
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
		return null;
	}
	private static final ZipShort ID = new ZipShort(0xC6FA);
	private final byte[] data;

	private MetaTypeExtraField(String code) {
		try {
			this.data = code.getBytes("UTF-8");
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