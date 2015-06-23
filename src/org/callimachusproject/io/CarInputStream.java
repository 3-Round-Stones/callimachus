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
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.text.Normalizer;
import java.util.regex.Pattern;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.io.LatencyInputStream;
import org.openrdf.http.object.util.URLUtil;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.helpers.StatementCollector;

public class CarInputStream implements Closeable {
	private static final int RDFS_PEEK_SIZE = 1024 * 1024;
	private static final Pattern SOURCE_NAME = Pattern
			.compile("[^/]+\\.source\\.ttl$");
	private static final Pattern FILE_NAME = Pattern
			.compile("[^/]+\\.[a-zA-Z]\\w*$");
	private final ZipArchiveInputStream zipStream;
	private final FileTypeMap mimetypes;
	ZipArchiveEntry entry;
	private MetaTypeExtraField entryMetaType;
	private String entryType;
	private BufferedInputStream entryStream;
	private final String base;
	boolean advance;

	public CarInputStream(File file) throws IOException {
		this(new FileInputStream(file), file.toURI().toASCIIString());
	}

	public CarInputStream(InputStream in, String base) throws IOException {
		zipStream = new ZipArchiveInputStream(in);
		mimetypes = MimetypesFileTypeMap.getDefaultFileTypeMap();
		this.base = base;
	}

	public void close() throws IOException {
		zipStream.close();
	}

	public synchronized String readEntryName() throws IOException {
		if (entry == null || advance) {
			entry = next();
		}
		if (entry == null)
			return null;
		return entry.getName();
	}

	public synchronized String getEntryName() throws IOException {
		return entry.getName();
	}

	public synchronized String getEntryIRI() throws IOException {
		String decoded = PercentCodec.decode(entry.getName());
		String normalized = Normalizer.normalize(decoded, Normalizer.Form.NFD);
		String encoded = PercentCodec.encodeOthers(normalized);
		String cleaned = encoded.replaceAll("%20", "+").replace('#', '_');
		return URLUtil.resolve(cleaned, base);
	}

	public synchronized long getEntryTime() throws IOException {
		if (entry == null)
			return -1;
		return entry.getTime();
	}

	public synchronized String getEntryType() throws IOException {
		return entryType;
	}

	public synchronized boolean isFolderEntry() throws IOException {
		return MetaTypeExtraField.FOLDER == entryMetaType;
	}

	public synchronized boolean isSourceEntry() throws IOException {
		return MetaTypeExtraField.SOURCE == entryMetaType;
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
		return entryStream;
	}

	public synchronized Model getEntryModel(ValueFactory vf) throws IOException, OpenRDFException {
		final Model model = new LinkedHashModel();
		RDFFormat format = Rio.getParserFormatForMIMEType(getEntryType());
		RDFParserRegistry registry = RDFParserRegistry.getInstance();
		RDFParser parser = registry.get(format).getParser();
		parser.setValueFactory(vf);
		parser.setRDFHandler(new RDFHandlerBase() {
			public void handleNamespace(String prefix, String uri)
					throws RDFHandlerException {
				model.setNamespace(prefix, uri);
			}
			public void handleStatement(Statement st)
					throws RDFHandlerException {
				model.add(st);
			}
		});
		InputStream in = getEntryStream();
		try {
			parser.parse(in, getEntryIRI());
		} finally {
			in.close();
		}
		return model;
	}

	private ZipArchiveEntry next() throws IOException {
		if (Thread.interrupted())
			throw new InterruptedIOException();
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
					advance = true;
				}
			}
		}, RDFS_PEEK_SIZE);
		entryType = readEntryType(entry, entryStream);
		entryMetaType = MetaTypeExtraField.parseExtraField(entry);
		if (entryMetaType == null) {
			if (entry.isDirectory()) {
				entryMetaType = MetaTypeExtraField.FOLDER;
			} else if (SOURCE_NAME.matcher(entry.getName()).find()) {
				entryMetaType = MetaTypeExtraField.SOURCE;
			} else if (FILE_NAME.matcher(entry.getName()).find()) {
				entryMetaType = MetaTypeExtraField.FILE;
			} else if (scanForClass(entryStream, entryType)) {
				entryMetaType = MetaTypeExtraField.RDFS;
			} else {
				entryMetaType = MetaTypeExtraField.RDF;
			}
		}
		return entry;
	}

	private String readEntryType(ZipArchiveEntry entry, BufferedInputStream in) throws IOException {
		if (entry == null)
			return null;
		String type = ContentTypeExtraField.parseExtraField(entry);
		if (type != null || entry.isDirectory())
			return type;
		if (FILE_NAME.matcher(entry.getName()).find())
			return mimetypes.getContentType(entry.getName());
		return detectRdfType(in);
	}

	private String detectRdfType(BufferedInputStream in) throws IOException {
		byte[] peek = new byte[200];
		in.mark(200);
		int len = IOUtil.readBytes(in, peek);
		in.reset();
		TextReader reader = new TextReader(new ByteArrayInputStream(peek, 0, len));
		try {
			int first = reader.read();
			if (first == '<')
				return "application/rdf+xml";
			return "text/turtle";
		} finally {
			reader.close();
		}
	}

	private boolean scanForClass(BufferedInputStream in, String type) throws IOException {
		assert type != null;
		byte[] peek = new byte[RDFS_PEEK_SIZE];
		in.mark(RDFS_PEEK_SIZE);
		int len = IOUtil.readBytes(in, peek);
		in.reset();
		URI uri = new URIImpl("http://example.com/" + entry.getName());
		LinkedHashModel model = new LinkedHashModel();
		try {
			RDFParserRegistry registry = org.openrdf.rio.RDFParserRegistry.getInstance();
			RDFParser parser = registry.get(registry.getFileFormatForMIMEType(type)).getParser();
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
