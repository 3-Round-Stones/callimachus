/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
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