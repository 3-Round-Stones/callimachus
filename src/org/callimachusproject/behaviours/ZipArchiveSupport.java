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
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.activation.MimetypesFileTypeMap;
import javax.tools.FileObject;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.callimachusproject.engine.helpers.XMLEventReaderBase;
import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.http.object.exceptions.BadRequest;

public abstract class ZipArchiveSupport implements CalliObject, FileObject {
	static final QName FEED = new QName("http://www.w3.org/2005/Atom", "feed");
	static final QName TITLE = new QName("http://www.w3.org/2005/Atom", "title");
	static final QName ID = new QName("http://www.w3.org/2005/Atom", "id");
	static final QName LINK = new QName("http://www.w3.org/2005/Atom", "link");
	static final QName UPDATED = new QName("http://www.w3.org/2005/Atom", "updated");
	static final QName ENTRY = new QName("http://www.w3.org/2005/Atom", "entry");
	/* NotThreadSafe */
	static final DateFormat ISO_8601;
	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		ISO_8601.setTimeZone(tz);
	}

	public void validateZipAndClose(InputStream in) throws IOException {
		ZipArchiveInputStream zip = new ZipArchiveInputStream(in);
		try {
			byte[] buf = new byte[1024];
			ZipArchiveEntry entry = zip.getNextZipEntry();
			if (entry == null) throw new BadRequest("Archive is empty");
			do {
				entry.getName();
				entry.getMethod();
				entry.getSize();
				while (zip.read(buf, 0, buf.length) >= 0)
					;
				entry = zip.getNextZipEntry();
			} while (entry != null);
		} finally {
			zip.close();
		}
	}

	public String getZipEntryType(String entry) {
        MimetypesFileTypeMap mimetypes = new javax.activation.MimetypesFileTypeMap();
        String type = mimetypes.getContentType(entry);
        if (type == null || type.length() == 0) {
        	return "application/octet-stream";
        }
        return type;
	}

	public InputStream readZipEntry(String match) throws IOException {
		boolean close = true;
		InputStream in = this.openInputStream();
		try {
			ZipArchiveInputStream zip = new ZipArchiveInputStream(in);
			byte[] buf = new byte[1024];
			ZipArchiveEntry entry = zip.getNextZipEntry();
			do {
				if (entry.getName().equals(match)) {
					close = false;
					return zip;
				}
				long size = entry.getSize();
				if (size > 0) {
					zip.skip(size);
				} else {
					while (zip.read(buf, 0, buf.length) >= 0)
						;
				}
				entry = zip.getNextZipEntry();
			} while (entry != null);
			zip.close();
		} finally {
			if (close) {
				in.close();
			}
		}
		return null;
	}

	public XMLEventReader createAtomFeedFromArchive(final String id, final String entryPattern) throws IOException {
		final FileObject file = this;
		final XMLEventFactory ef = XMLEventFactory.newInstance();
		final byte[] buf = new byte[1024];
		final ZipArchiveInputStream zip = new ZipArchiveInputStream(file.openInputStream());
		return new XMLEventReaderBase() {
			private boolean started;
			private boolean ended;
			public void close() throws XMLStreamException {
				try {
					zip.close();
				} catch (IOException e) {
					throw new XMLStreamException(e);
				}
			}
			protected boolean more() throws XMLStreamException {
				try {
					ZipArchiveEntry entry;
					if (!started) {
						Namespace atom = ef.createNamespace(FEED.getPrefix(), FEED.getNamespaceURI());
						add(ef.createStartDocument());
						add(ef.createStartElement(FEED, null, Arrays.asList(atom).iterator()));
						add(ef.createStartElement(TITLE, null, null));
						add(ef.createCharacters(file.getName()));
						add(ef.createEndElement(TITLE, null));
						add(ef.createStartElement(ID, null, null));
						add(ef.createCharacters(id));
						add(ef.createEndElement(ID, null));
						Attribute href = ef.createAttribute("href", file.toUri().toASCIIString());
						List<Attribute> attrs = Arrays.asList(href, ef.createAttribute("type", "application/zip"));
						add(ef.createStartElement(LINK, attrs.iterator(), null));
						add(ef.createEndElement(LINK, null));
						add(ef.createStartElement(UPDATED, null, null));
						add(ef.createCharacters(format(new Date(file.getLastModified()))));
						add(ef.createEndElement(UPDATED, null));
						started = true;
						return true;
					} else if (started && !ended && (entry = zip.getNextZipEntry()) != null) {
						String name = entry.getName();
						String link = entryPattern.replace("{entry}", PercentCodec.encode(name));
		                MimetypesFileTypeMap mimetypes = new javax.activation.MimetypesFileTypeMap();
		                String type = mimetypes.getContentType(name);
		                if (type == null || type.length() == 0) {
		                	type = "application/octet-stream";
		                }
						add(ef.createStartElement(ENTRY, null, null));
						add(ef.createStartElement(TITLE, null, null));
						add(ef.createCharacters(name));
						add(ef.createEndElement(TITLE, null));
						Attribute href = ef.createAttribute("href", link);
						List<Attribute> attrs = Arrays.asList(href, ef.createAttribute("type", type));
						add(ef.createStartElement(LINK, attrs.iterator(), null));
						add(ef.createEndElement(LINK, null));
						long size = entry.getSize();
						if (size > 0) {
							zip.skip(size);
						} else {
							while (zip.read(buf, 0, buf.length) >= 0)
								;
						}
						add(ef.createEndElement(ENTRY, null));
						return true;
					} else if (!ended) {
						add(ef.createEndElement(FEED, null));
						add(ef.createEndDocument());
						ended = true;
						return true;
					} else {
						return false;
					}
				} catch (IOException e) {
					throw new XMLStreamException(e);
				}
			}
		};
	}

	String format(Date date) {
		synchronized (ISO_8601) {
			return ISO_8601.format(date);
		}
	}
}
