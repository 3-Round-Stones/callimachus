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

import info.aduna.io.IOUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.rdfxml.RDFXMLParser;

public class CarInputStream implements Closeable {
	private static final int RDFS_PEEK_SIZE = 1024 * 1024;
	private static final Pattern FILE_NAME = Pattern
			.compile("[^/]+\\.[a-zA-Z]+$");
	private final ZipArchiveInputStream zipStream;
	private final FileTypeMap mimetypes;
	private ZipArchiveEntry entry;
	private MetaTypeExtraField entryMetaType;
	private BufferedInputStream entryStream;

	public CarInputStream(InputStream in) throws IOException {
		zipStream = new ZipArchiveInputStream(in);
		mimetypes = MimetypesFileTypeMap.getDefaultFileTypeMap();
	}

	public void close() throws IOException {
		zipStream.close();
	}

	public synchronized String readEntryName() throws IOException {
		if (entry == null) {
			entry = next();
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
		String type = ContentTypeExtraField.parseExtraField(entry);
		if (type != null)
			return type;
		if (isFileEntry())
			return mimetypes.getContentType(entry.getName());
		if (isResourceEntry() || isSchemaEntry())
			return "application/rdf+xml";
		return null;
	}

	public synchronized boolean isFolderEntry() throws IOException {
		return MetaTypeExtraField.FOLDER == entryMetaType;
	}

	public synchronized boolean isResourceEntry() throws IOException {
		return MetaTypeExtraField.RDF == entryMetaType;
	}

	public synchronized boolean isSchemaEntry() throws IOException {
		return MetaTypeExtraField.RDFS == entryMetaType;
	}

	public synchronized boolean isFileEntry() throws IOException {
		return MetaTypeExtraField.FILE == entryMetaType;
	}

	public synchronized InputStream getEntryStream() throws IOException {
		if (entryStream == null)
			return null;
		return entryStream;
	}

	private ZipArchiveEntry next() throws IOException {
		entry = zipStream.getNextZipEntry();
		if (entry == null) {
			entryStream = null;
			entryMetaType = null;
			return null;
		}
		final ZipArchiveEntry openEntry = entry;
		entryStream = new LatencyInputStream(new FilterInputStream(zipStream) {
			public void close() throws IOException {
				if (openEntry == entry) {
					entry = null;
					entryStream = null;
					entryMetaType = null;
				}
			}
		}, RDFS_PEEK_SIZE);
		entryMetaType = MetaTypeExtraField.parseExtraField(entry);
		if (entryMetaType == null) {
			if (entry.isDirectory()) {
				entryMetaType = MetaTypeExtraField.FOLDER;
			} else if (FILE_NAME.matcher(entry.getName()).find()) {
				entryMetaType = MetaTypeExtraField.FILE;
			} else if (scanForClass(entryStream)) {
				entryMetaType = MetaTypeExtraField.RDFS;
			} else {
				entryMetaType = MetaTypeExtraField.RDF;
			}
		}
		return entry;
	}

	private boolean scanForClass(BufferedInputStream in) throws IOException {
		byte[] peek = new byte[RDFS_PEEK_SIZE];
		in.mark(RDFS_PEEK_SIZE);
		int len = IOUtil.readBytes(in, peek);
		in.reset();
		URI uri = new URIImpl("http://example.com/" + entry.getName());
		LinkedHashModel model = new LinkedHashModel();
		try {
			RDFXMLParser parser = new RDFXMLParser();
			parser.setRDFHandler(new StatementCollector(model));
			parser.parse(new ByteArrayInputStream(peek, 0, len), uri.toString());
		} catch (RDFParseException e) {
			// ignore
		} catch (RDFHandlerException e) {
			// ignore
		}
		return model.contains(uri, RDF.TYPE, OWL.ONTOLOGY)
				|| model.contains(uri, RDF.TYPE, OWL.CLASS)
				|| model.contains(uri, RDF.TYPE, OWL.OBJECTPROPERTY)
				|| model.contains(uri, RDF.TYPE, OWL.DATATYPEPROPERTY)
				|| model.contains(uri, RDF.TYPE, OWL.FUNCTIONALPROPERTY)
				|| model.contains(uri, RDF.TYPE, RDFS.CLASS)
				|| model.contains(uri, RDF.TYPE, RDF.PROPERTY);
	}

}
