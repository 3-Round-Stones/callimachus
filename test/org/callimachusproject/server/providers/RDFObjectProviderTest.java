package org.callimachusproject.server.providers;

import java.util.Set;

import org.callimachusproject.server.annotations.query;
import org.callimachusproject.server.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.DescribeSupport;
import org.openrdf.annotations.Iri;
import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectConnection;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class RDFObjectProviderTest extends MetadataServerTestCase {

	private ObjectConnection con;

	@Iri("urn:test:Document")
	public interface Document {
		@query("author")
		@type("application/rdf+xml")
		@Iri("urn:test:author")
		Person getAuthor();
		@query("author")
		@Iri("urn:test:author")
		void setAuthor(@type("application/rdf+xml") Person author);
		@query("contributors")
		@type("application/rdf+xml")
		@Iri("urn:test:contributor")
		Set<Person> getContributors();
		@query("contributors")
		@Iri("urn:test:contributor")
		void setContributors(@type("application/rdf+xml") Set<Person> contributors);
	}

	@Iri("urn:test:Person")
	public interface Person {
		@Iri("urn:test:name")
		String getName();
		void setName(String name);
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Document.class);
		config.addConcept(Person.class);
		config.addBehaviour(DescribeSupport.class, RDFS.RESOURCE);
		super.setUp();
		con = repository.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public void testNamedAuthor() throws Exception {
		Person author = con.addDesignation(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addDesignation(con.getObject(base+"/doc"), Document.class).setAuthor(author);
		WebResource web = client.path("/doc").queryParam("author", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAnonyoumsAuthor() throws Exception {
		Person author = con.addDesignation(con.getObjectFactory().createObject(), Person.class);
		author.setName("James");
		con.addDesignation(con.getObject(base+"/doc"), Document.class).setAuthor(author);
		WebResource web = client.path("/doc").queryParam("author", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingNamedAuthor() throws Exception {
		Person author = con.addDesignation(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addDesignation(con.getObject(base+"/doc"), Document.class);
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		web.header("Content-Location", base+"/auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testRemovingNamedAuthor() throws Exception {
		Person author = con.addDesignation(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addDesignation(con.getObject(base+"/doc"), Document.class).setAuthor(author);
		WebResource web = client.path("/doc").queryParam("author", "");
		web.delete();
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testAddingRelativeAuthor() throws Exception {
		Person author = con.addDesignation(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addDesignation(con.getObject(base+"/doc"), Document.class);
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		web.header("Content-Location", "auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingAnonyoumsAuthor() throws Exception {
		con.addDesignation(con.getObject(base+"/doc"), Document.class);
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		Model model = new LinkedHashModel();
		BNode auth = vf.createBNode();
		model.add(auth, RDF.TYPE, vf.createURI("urn:test:Person"));
		model.add(auth, vf.createURI("urn:test:name"), vf.createLiteral("James"));
		web.type("application/rdf+xml").put(model);
		model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testNamedContributors() throws Exception {
		Document document = con.addDesignation(con.getObject(base+"/doc"), Document.class);
		Person contributor = con.addDesignation(con.getObject(base+"/james"), Person.class);
		contributor.setName("James");
		document.getContributors().add(contributor);
		contributor = con.addDesignation(con.getObject(base+"/megan"), Person.class);
		contributor.setName("Megan");
		document.getContributors().add(contributor);
		WebResource web = client.path("/doc").queryParam("contributors", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("Megan")));
	}

	public void testAnonyoumsContributors() throws Exception {
		Document document = con.addDesignation(con.getObject(base+"/doc"), Document.class);
		Person contributor = con.addDesignation(con.getObjectFactory().createObject(), Person.class);
		contributor.setName("James");
		document.getContributors().add(contributor);
		contributor = con.addDesignation(con.getObjectFactory().createObject(), Person.class);
		contributor.setName("Megan");
		document.getContributors().add(contributor);
		WebResource web = client.path("/doc").queryParam("contributors", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("Megan")));
	}

	public void testAddingNamedContributor() throws Exception {
		Person contributors = con.addDesignation(con.getObject(base+"/auth"), Person.class);
		contributors.setName("James");
		con.addDesignation(con.getObject(base+"/doc"), Document.class);
		WebResource web = client.path("/doc").queryParam("contributors", "");
		web.header("Content-Location", base+"/auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testRemovingNamedContributors() throws Exception {
		Person contributor = con.addDesignation(con.getObject(base+"/auth"), Person.class);
		contributor.setName("James");
		con.addDesignation(con.getObject(base+"/doc"), Document.class).getContributors().add(contributor);
		WebResource web = client.path("/doc").queryParam("contributors", "");
		web.delete();
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testAddingRelativeContributor() throws Exception {
		Person contributors = con.addDesignation(con.getObject(base+"/auth"), Person.class);
		contributors.setName("James");
		con.addDesignation(con.getObject(base+"/doc"), Document.class);
		WebResource web = client.path("/doc").queryParam("contributors", "");
		web.header("Content-Location", "auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingAnonyoumsContributors() throws Exception {
		con.addDesignation(con.getObject(base+"/doc"), Document.class);
		WebResource web = client.path("/doc").queryParam("contributors", "");
		Model model = new LinkedHashModel();
		BNode auth = vf.createBNode();
		model.add(auth, RDF.TYPE, vf.createURI("urn:test:Person"));
		model.add(auth, vf.createURI("urn:test:name"), vf.createLiteral("James"));
		auth = vf.createBNode();
		model.add(auth, RDF.TYPE, vf.createURI("urn:test:Person"));
		model.add(auth, vf.createURI("urn:test:name"), vf.createLiteral("Megan"));
		web.type("application/rdf+xml").put(model);
		model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("Megan")));
	}
}
