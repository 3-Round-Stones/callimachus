/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.callimachusproject.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RepositoryUtil;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.RDFHandlerWrapper;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.sail.memory.MemoryStore;

/**
 * @author Arjohn Kampman
 */
public abstract class RDFWriterTestCase {

	protected RDFWriterFactory rdfWriterFactory;

	protected RDFParserFactory rdfParserFactory;

	protected RDFWriterTestCase(RDFWriterFactory writerF, RDFParserFactory parserF) {
		rdfWriterFactory = writerF;
		rdfParserFactory = parserF;
	}

	@Test
	public void testRoundTrip()
		throws RDFHandlerException, IOException, RDFParseException
	{
		String ex = "http://example.org/";

		ValueFactory vf = new ValueFactoryImpl();
		BNode bnode = vf.createBNode("anon");
		URI uri1 = vf.createURI(ex, "uri1");
		URI uri2 = vf.createURI(ex, "uri2");
		Literal plainLit = vf.createLiteral("plain");
		Literal dtLit = vf.createLiteral(1);
		Literal langLit = vf.createLiteral("test", "en");
		Literal litWithSingleQuotes = vf.createLiteral("'''some single quote text''' - abc");
		Literal litWithDoubleQuotes = vf.createLiteral("\"\"\"some double quote text\"\"\" - abc");

		Statement st1 = vf.createStatement(bnode, uri1, plainLit);
		Statement st2 = vf.createStatement(uri1, uri2, langLit, uri2);
		Statement st3 = vf.createStatement(uri1, uri2, dtLit);
		Statement st7 = vf.createStatement(uri1, uri2, litWithSingleQuotes);
		Statement st8 = vf.createStatement(uri1, uri2, litWithDoubleQuotes);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		rdfWriter.handleNamespace("ex", ex);
		rdfWriter.startRDF();
		rdfWriter.handleStatement(st1);
		rdfWriter.handleStatement(st2);
		rdfWriter.handleStatement(st3);
		rdfWriter.handleStatement(st7);
		rdfWriter.handleStatement(st8);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		ParserConfig config = new ParserConfig();
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		rdfParser.setParserConfig(config);
		rdfParser.setValueFactory(vf);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));

		rdfParser.parse(in, "foo:bar");

		assertEquals("Unexpected number of statements", 5, model.size());
		// assertTrue(statements.contains(st1));
		if (rdfParser.getRDFFormat().supportsContexts()) {
			assertTrue(model.contains(st2));
		}
		else {
			assertTrue(model.contains(vf.createStatement(uri1, uri2, langLit)));
		}
		assertTrue(model.contains(st3));
		assertTrue("missing statement with single quotes", model.contains(st7));
		assertTrue("missing statement with double quotes", model.contains(st8));
	}

	@Test
	public void testPrefixRedefinition()
		throws RDFHandlerException, RDFParseException, IOException
	{
		String ns1 = "a:";
		String ns2 = "b:";
		String ns3 = "c:";

		ValueFactory vf = new ValueFactoryImpl();
		URI uri1 = vf.createURI(ns1, "r1");
		URI uri2 = vf.createURI(ns2, "r2");
		URI uri3 = vf.createURI(ns3, "r3");
		Statement st = vf.createStatement(uri1, uri2, uri3);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		rdfWriter.handleNamespace("", ns1);
		rdfWriter.handleNamespace("", ns2);
		rdfWriter.handleNamespace("", ns3);
		rdfWriter.startRDF();
		rdfWriter.handleStatement(st);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		rdfParser.setValueFactory(vf);
		StatementCollector stCollector = new StatementCollector();
		rdfParser.setRDFHandler(stCollector);

		rdfParser.parse(in, "foo:bar");

		Collection<Statement> statements = stCollector.getStatements();
		assertEquals("Unexpected number of statements", 1, statements.size());

		Statement parsedSt = statements.iterator().next();
		assertEquals("Written and parsed statements are not equal", st, parsedSt);
	}

	@Test
	public void testIllegalPrefix()
		throws RDFHandlerException, RDFParseException, IOException
	{
		String ns1 = "a:";
		String ns2 = "b:";
		String ns3 = "c:";

		ValueFactory vf = new ValueFactoryImpl();
		URI uri1 = vf.createURI(ns1, "r1");
		URI uri2 = vf.createURI(ns2, "r2");
		URI uri3 = vf.createURI(ns3, "r3");
		Statement st = vf.createStatement(uri1, uri2, uri3);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		rdfWriter.handleNamespace("1", ns1);
		rdfWriter.handleNamespace("_", ns2);
		rdfWriter.handleNamespace("a%", ns3);
		rdfWriter.startRDF();
		rdfWriter.handleStatement(st);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		rdfParser.setValueFactory(vf);
		StatementCollector stCollector = new StatementCollector();
		rdfParser.setRDFHandler(stCollector);

		rdfParser.parse(in, "foo:bar");

		Collection<Statement> statements = stCollector.getStatements();
		assertEquals("Unexpected number of statements", 1, statements.size());

		Statement parsedSt = statements.iterator().next();
		assertEquals("Written and parsed statements are not equal", st, parsedSt);
	}

	@Test
	public void testDefaultNamespace()
		throws Exception
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		rdfWriter.handleNamespace("", RDF.NAMESPACE);
		rdfWriter.handleNamespace("rdf", RDF.NAMESPACE);
		rdfWriter.startRDF();
		rdfWriter.handleStatement(new StatementImpl(new URIImpl(RDF.NAMESPACE), RDF.TYPE, OWL.ONTOLOGY));
		rdfWriter.endRDF();
	}

	@Test
	public void testRepoConfig()
		throws RepositoryException, RDFParseException, IOException, RDFHandlerException
	{
		Repository rep1 = new SailRepository(new MemoryStore());
		rep1.initialize();

		RepositoryConnection con1 = rep1.getConnection();

		InputStream config = new FileInputStream("src/callimachus-repository.ttl");

		con1.add(config, new File("src/callimachus-repository.ttl").toURI().toASCIIString(), RDFFormat.TURTLE);

		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);
		con1.export(rdfWriter);

		con1.close();

		Repository rep2 = new SailRepository(new MemoryStore());
		rep2.initialize();

		RepositoryConnection con2 = rep2.getConnection();

		// System.out.println(writer.toString());
		con2.add(new StringReader(writer.toString()), "http://example.org/", rdfWriterFactory.getRDFFormat());
		con2.close();

		Assert.assertTrue("result of serialization and re-upload should be equal to original", RepositoryUtil.equals(
				rep1, rep2));
	}

	@Test
	public void testFactbook()
		throws RepositoryException, RDFParseException, IOException, RDFHandlerException
	{
		Repository rep1 = new SailRepository(new MemoryStore());
		rep1.initialize();

		RepositoryConnection con1 = rep1.getConnection();

		InputStream ciaScheme = this.getClass().getResourceAsStream("/cia-factbook/CIA-onto-enhanced.rdf");
		InputStream ciaFacts = this.getClass().getResourceAsStream("/cia-factbook/CIA-facts-enhanced.rdf");

		con1.add(ciaScheme, "urn:cia-factbook/CIA-onto-enhanced.rdf", RDFFormat.RDFXML);
		con1.add(ciaFacts, "urn:cia-factbook/CIA-facts-enhanced.rdf", RDFFormat.RDFXML);

		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);
		con1.export(rdfWriter);

		con1.close();

		Repository rep2 = new SailRepository(new MemoryStore());
		rep2.initialize();

		RepositoryConnection con2 = rep2.getConnection();

		con2.add(new StringReader(writer.toString()), "http://example.org/", rdfWriterFactory.getRDFFormat());
		con2.close();

		Assert.assertTrue("result of serialization and re-upload should be equal to original", RepositoryUtil.equals(
				rep1, rep2));
	}

	@Test
	public void testBlankNodes() throws Exception {
		String ex = "http://example.org/";

		ValueFactory vf = new ValueFactoryImpl();
		BNode b1 = vf.createBNode("b1");
		BNode b2 = vf.createBNode("b2");
		BNode b3 = vf.createBNode("b3");
		BNode b4 = vf.createBNode("b4");
		BNode b5 = vf.createBNode("b5");
		BNode b6 = vf.createBNode("b6");
		URI uri1 = vf.createURI(ex, "uri1");
		URI uri2 = vf.createURI(ex, "uri2");
		Literal plain2 = vf.createLiteral("plain2");
		Literal plain3 = vf.createLiteral("plain3");
		Literal plain4 = vf.createLiteral("plain4");
		Literal plain5 = vf.createLiteral("plain5");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StatementCollector coll = new StatementCollector();
		RDFHandler rdfWriter = new RDFHandlerWrapper(rdfWriterFactory.getWriter(out), coll);
		rdfWriter.handleNamespace("ex", ex);
		rdfWriter.startRDF();
		rdfWriter.handleStatement(new StatementImpl(uri1, uri1, b1));
		rdfWriter.handleStatement(new StatementImpl(uri1, uri1, b2));
		rdfWriter.handleStatement(new StatementImpl(b2, uri1, plain2));
		rdfWriter.handleStatement(new StatementImpl(uri1, uri1, b3));
		rdfWriter.handleStatement(new StatementImpl(b3, uri1, plain3));
		rdfWriter.handleStatement(new StatementImpl(b3, uri1, uri2));
		rdfWriter.handleStatement(new StatementImpl(uri1, uri1, b4));
		rdfWriter.handleStatement(new StatementImpl(b4, uri1, plain4));
		rdfWriter.handleStatement(new StatementImpl(b4, uri2, b5));
		rdfWriter.handleStatement(new StatementImpl(b5, uri1, plain5));
		rdfWriter.handleStatement(new StatementImpl(b4, uri2, b6));
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		ParserConfig config = new ParserConfig();
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		rdfParser.setParserConfig(config);
		rdfParser.setValueFactory(vf);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));

		// System.out.println(new String(out.toByteArray(), "UTF-8"));
		rdfParser.parse(in, "http://example.org/");

		assertTrue(ModelUtil.equals(coll.getStatements(), model));
	}
}
