package org.callimachusproject.behaivours;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.callimachusproject.behaviours.FormSupport;
import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.TemplateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class FormSupportTest {
	private static final String SKOS = "http://www.w3.org/2004/02/skos/core#";
	private XMLInputFactory xif = XMLInputFactory.newInstance();
	private ObjectRepository repo;
	private ObjectConnection con;

	public class FormSupportImpl extends FormSupport {
		private Resource self;
		private String xml;

		public FormSupportImpl(Resource self, String xml) {
			this.self = self;
			this.xml = xml;
		}

		@Override
		public XMLEventReader calliConstruct(Object target)
				throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RDFEventReader openPatternReader(String about, String element) throws IOException, TemplateException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ObjectConnection getObjectConnection() {
			return con;
		}

		@Override
		public Resource getResource() {
			return self;
		}

		public String toString() {
			return self.stringValue();
		}

		@Override
		protected XMLEventReader xslt(String element)
				throws IOException, XMLStreamException {
			return xif.createXMLEventReader(new StringReader(xml));
		}
		
	}

	@Before
	public void setUp() throws Exception {
		SailRepository sail = new SailRepository(new MemoryStore());
		sail.initialize();
		repo = new ObjectRepositoryFactory().createRepository(sail);
		con = repo.getConnection();
	}

	@After
	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	@Test
	public void testOptions1() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' /></select>";
		InputStream in = new FormSupportImpl(vf.createURI("urn:test"), xml).options("/1/2/1/14/1");
		XMLEventReader options = xif.createFilteredReader(xif.createXMLEventReader(in), new EventFilter() {
			public boolean accept(XMLEvent event) {
				return event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("option");
			}
		});
		assertTrue(options.hasNext());
		options.next();
		assertTrue(!options.hasNext());
	}

	@Test
	public void testOptions2() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept1"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept1"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label"));
		con.add(vf.createURI("urn:test:concept2"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept2"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' /></select>";
		InputStream in = new FormSupportImpl(vf.createURI("urn:test"), xml).options("/1/2/1/14/1");
		XMLEventReader options = xif.createFilteredReader(xif.createXMLEventReader(in), new EventFilter() {
			public boolean accept(XMLEvent event) {
				return event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("option");
			}
		});
		assertTrue(options.hasNext());
		options.next();
		assertTrue(options.hasNext());
		options.next();
		assertTrue(!options.hasNext());
	}

	@Test
	public void testOptions3() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept1"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept1"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label"));
		con.add(vf.createURI("urn:test:concept2"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept2"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label"));
		con.add(vf.createURI("urn:test:concept3"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' /></select>";
		InputStream in = new FormSupportImpl(vf.createURI("urn:test"), xml).options("/1/2/1/14/1");
		XMLEventReader options = xif.createFilteredReader(xif.createXMLEventReader(in), new EventFilter() {
			public boolean accept(XMLEvent event) {
				return event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("option");
			}
		});
		assertTrue(options.hasNext());
		options.next();
		assertTrue(options.hasNext());
		options.next();
		assertTrue(!options.hasNext());
	}

	@Test
	@Ignore
	public void testOptions4() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept1"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept1"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label"));
		con.add(vf.createURI("urn:test:concept2"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept3"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept3"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' /></select>";
		InputStream in = new FormSupportImpl(vf.createURI("urn:test"), xml).options("/1/2/1/14/1");
		XMLEventReader options = xif.createFilteredReader(xif.createXMLEventReader(in), new EventFilter() {
			public boolean accept(XMLEvent event) {
				return event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("option");
			}
		});
		assertTrue(options.hasNext());
		options.next();
		assertTrue(options.hasNext());
		options.next();
		assertTrue(!options.hasNext());
	}
}
