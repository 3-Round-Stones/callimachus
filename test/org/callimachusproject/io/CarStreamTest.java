package org.callimachusproject.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import org.callimachusproject.rio.RDFXMLStreamWriterFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;

public class CarStreamTest {
	private ValueFactory vf = ValueFactoryImpl.getInstance();
	private File car;

	@Before
	public void setUp() throws Exception {
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr != null) {
			File tmpDir = new File(tmpDirStr);
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
		}
		car = File.createTempFile("test", ".car");
	}

	@After
	public void tearDown() throws Exception {
		car.delete();
	}

	@Test
	public void testRoundTripEmptyFolder() throws FileNotFoundException, IOException {
		long now = System.currentTimeMillis() / 2000 * 2000;
		CarOutputStream out = new CarOutputStream(new FileOutputStream(car));
		out.writeFolderEntry("dir/", now).close();
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("dir/", in.readEntryName());
		assertTrue(in.isFolderEntry());
		assertEquals(now, in.getEntryTime());
		assertNull(in.getEntryType());
		in.getEntryStream().close();
		assertNull(in.readEntryName());
		in.close();
	}

	@Test
	public void testRoundTripEmptyFile() throws FileNotFoundException, IOException {
		long now = System.currentTimeMillis() / 2000 * 2000;
		CarOutputStream out = new CarOutputStream(new FileOutputStream(car));
		out.writeFileEntry("file", now, "text/plain").close();
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("file", in.readEntryName());
		assertTrue(in.isFileEntry());
		assertEquals(now, in.getEntryTime());
		assertEquals("text/plain", in.getEntryType());
		InputStream es = in.getEntryStream();
		assertEquals(-1, es.read());
		es.close();
		assertNull(in.readEntryName());
		in.close();
	}

	@Test
	public void testRoundTripEmptyResource() throws FileNotFoundException, IOException {
		long now = System.currentTimeMillis() / 2000 * 2000;
		CarOutputStream out = new CarOutputStream(new FileOutputStream(car));
		out.writeResourceEntry("resource", now, "text/plain").close();
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("resource", in.readEntryName());
		assertTrue(in.isResourceEntry());
		assertEquals(now, in.getEntryTime());
		assertEquals("text/plain", in.getEntryType());
		InputStream es = in.getEntryStream();
		assertEquals(-1, es.read());
		es.close();
		assertNull(in.readEntryName());
		in.close();
	}

	@Test
	public void testRoundTripEmptySchema() throws FileNotFoundException, IOException {
		long now = System.currentTimeMillis() / 2000 * 2000;
		CarOutputStream out = new CarOutputStream(new FileOutputStream(car));
		out.writeSchemaEntry("schema", now, "text/plain").close();
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("schema", in.readEntryName());
		assertTrue(in.isSchemaEntry());
		assertEquals(now, in.getEntryTime());
		assertEquals("text/plain", in.getEntryType());
		InputStream es = in.getEntryStream();
		assertEquals(-1, es.read());
		es.close();
		assertNull(in.readEntryName());
		in.close();
	}

	@Test
	public void testRoundTripEverythingEmpty() throws FileNotFoundException, IOException {
		long now = System.currentTimeMillis() / 2000 * 2000;
		CarOutputStream out = new CarOutputStream(new FileOutputStream(car));
		out.writeFolderEntry("dir/", now).close();
		out.writeFileEntry("file", now, "text/plain").close();
		out.writeResourceEntry("resource", now, "text/plain").close();
		out.writeSchemaEntry("schema", now, "text/plain").close();
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("dir/", in.readEntryName());
		assertTrue(in.isFolderEntry());
		assertEquals(now, in.getEntryTime());
		assertNull(in.getEntryType());
		in.getEntryStream().close();
		assertEquals("file", in.readEntryName());
		assertTrue(in.isFileEntry());
		assertEquals(now, in.getEntryTime());
		assertEquals("text/plain", in.getEntryType());
		InputStream es1 = in.getEntryStream();
		assertEquals(-1, es1.read());
		es1.close();
		assertEquals("resource", in.readEntryName());
		assertTrue(in.isResourceEntry());
		assertEquals(now, in.getEntryTime());
		assertEquals("text/plain", in.getEntryType());
		InputStream es2 = in.getEntryStream();
		assertEquals(-1, es2.read());
		es2.close();
		assertEquals("schema", in.readEntryName());
		assertTrue(in.isSchemaEntry());
		assertEquals(now, in.getEntryTime());
		assertEquals("text/plain", in.getEntryType());
		InputStream es3 = in.getEntryStream();
		assertEquals(-1, es3.read());
		es3.close();
		assertNull(in.readEntryName());
		in.close();
	}

	@Test
	public void testRoundTripExternalFolder() throws FileNotFoundException, IOException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(car));
		out.putNextEntry(new ZipEntry("dir/"));
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("dir/", in.readEntryName());
		assertTrue(in.isFolderEntry());
		assertNull(in.getEntryType());
		in.getEntryStream().close();
		assertNull(in.readEntryName());
		in.close();
	}

	@Test
	public void testRoundTripExternalFile() throws FileNotFoundException, IOException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(car));
		out.putNextEntry(new ZipEntry("file.txt"));
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("file.txt", in.readEntryName());
		assertTrue(in.isFileEntry());
		assertEquals("text/plain", in.getEntryType());
		InputStream es = in.getEntryStream();
		assertEquals(-1, es.read());
		es.close();
		assertNull(in.readEntryName());
		in.close();
	}

	@Test
	public void testRoundTripExternalResource() throws FileNotFoundException, IOException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(car));
		out.putNextEntry(new ZipEntry("resource"));
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("resource", in.readEntryName());
		assertTrue(in.isResourceEntry());
		assertEquals("application/rdf+xml", in.getEntryType());
		InputStream es = in.getEntryStream();
		assertEquals(-1, es.read());
		es.close();
		assertNull(in.readEntryName());
		in.close();
	}

	@Test
	public void testRoundTripExternalSchema() throws FileNotFoundException, IOException, XMLStreamException, RDFHandlerException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(car));
		out.putNextEntry(new ZipEntry("schema"));
		RDFWriter writer = new RDFXMLStreamWriterFactory().createWriter(out, "urn:test:schema");
		writer.startRDF();
		writer.handleStatement(new StatementImpl(vf.createURI("urn:test:schema"), RDF.TYPE, OWL.ONTOLOGY));
		writer.endRDF();
		out.close();
		CarInputStream in = new CarInputStream(new FileInputStream(car));
		assertEquals("schema", in.readEntryName());
		assertTrue(in.isSchemaEntry());
		assertEquals("application/rdf+xml", in.getEntryType());
		InputStream es = in.getEntryStream();
		es.close();
		assertNull(in.readEntryName());
		in.close();
	}

}
