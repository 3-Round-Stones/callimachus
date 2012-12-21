package org.callimachusproject.rewrite;

import java.net.URI;

import junit.framework.TestCase;

import org.apache.http.HttpResponse;
import org.callimachusproject.annotations.alternate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.annotations.Iri;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class RedirectTest extends TestCase {

	@Iri("http://example.com/Concept")
	public interface Concept {
		@alternate("http://example.com/")
		HttpResponse literal();

		@alternate("^http://(.*).example.com/(.*) http://example.com/$1/$2")
		HttpResponse readDomain();

		@alternate("$0#{+frag}")
		HttpResponse frag(@Iri("urn:test:frag") String frag);

		@alternate("$0?{+query}")
		HttpResponse replaceQuery(@Iri("urn:test:query") String query);

		@alternate("$0?param={value}")
		HttpResponse param(@Iri("urn:test:value") String value);

		@alternate("{+value}")
		HttpResponse resolve(@Iri("urn:test:value") java.net.URI value);
	}

	private Repository repository;
	private ObjectRepositoryConfig config = new ObjectRepositoryConfig();
	private ObjectConnection con;
	private Concept concept;

	private Repository createRepository() throws Exception {
		return new SailRepository(new MemoryStore());
	}

	private ObjectRepository getRepository() throws Exception {
		Repository repository = createRepository();
		repository.initialize();
		RepositoryConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			con.clear();
			con.clearNamespaces();
			con.setNamespace("test", "urn:test:");
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		return factory.createRepository(config, repository);
	}

	@Before
	public void setUp() throws Exception {
		config.addConcept(Concept.class);
		repository = getRepository();
		con = (ObjectConnection) repository.getConnection();
		con.setAutoCommit(false);
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.setNamespace("rdfs", RDFS.NAMESPACE);
		con.setAutoCommit(true);
	}

	@After
	public void tearDown() throws Exception {
		try {
			if (con.isOpen()) {
				con.close();
			}
			repository.shutDown();
		} catch (Exception e) {
		}
	}

	@Test
	public void testLiteral() throws Exception {
		concept = con.addDesignation(con.getObject("urn:test:concept"),
				Concept.class);
		assertEquals("http://example.com/", concept.literal().getFirstHeader("Location").getValue());
	}

	@Test
	public void testSubsitution() throws Exception {
		concept = con.addDesignation(con.getObject("http://www.example.com/pathinfo"),
				Concept.class);
		assertEquals("http://www.example.com/pathinfo#sub", concept.frag("sub").getFirstHeader("Location").getValue());
	}

	@Test
	public void testDomain() throws Exception {
		concept = con.addDesignation(con.getObject("http://www.example.com/pathinfo"),
				Concept.class);
		assertEquals("http://example.com/www/pathinfo", concept.readDomain().getFirstHeader("Location").getValue());
	}

	@Test
	public void testQueryString() throws Exception {
		concept = con.addDesignation(con.getObject("http://www.example.com/pathinfo"),
				Concept.class);
		assertEquals("http://www.example.com/pathinfo?qs", concept.replaceQuery("qs").getFirstHeader("Location").getValue());
	}

	@Test
	public void testQueryParameter() throws Exception {
		concept = con.addDesignation(con.getObject("http://www.example.com/pathinfo"),
				Concept.class);
		assertEquals("http://www.example.com/pathinfo?param=foo+bar", concept.param("foo bar").getFirstHeader("Location").getValue());
	}

	@Test
	public void testQueryParameterURIEncoding() throws Exception {
		concept = con.addDesignation(con.getObject("http://www.example.com/pathinfo"),
				Concept.class);
		assertEquals("http://example.com/%E2%9C%93", concept.resolve(URI.create("http://example.com/✓")).getFirstHeader("Location").getValue());
	}

}
