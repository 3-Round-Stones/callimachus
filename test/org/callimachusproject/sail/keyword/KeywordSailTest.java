package org.callimachusproject.sail.keyword;

import info.aduna.io.FileUtil;

import java.io.File;
import java.util.Collections;

import junit.framework.TestCase;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

public class KeywordSailTest extends TestCase {
	private static final String PREFIX = "PREFIX rdfs:<" + RDFS.NAMESPACE + ">\n"
			+ "PREFIX rdf:<" + RDF.NAMESPACE + ">\n"
			+ "PREFIX keyword:<http://www.openrdf.org/rdf/2011/keyword#>\n";
	private File dir;
	private RepositoryConnection con;
	private Repository repo;
	private ValueFactory vf;

	public void setUp() throws Exception {
		if (dir == null) {
			String tmpDirStr = System.getProperty("java.io.tmpdir");
			if (tmpDirStr != null) {
				File tmpDir = new File(tmpDirStr);
				if (!tmpDir.exists()) {
					tmpDir.mkdirs();
				}
			}
			dir = File.createTempFile("keyword", "");
			dir.delete();
			dir.mkdirs();
		}
		Sail sail = new KeywordSail(new MemoryStore(dir));
		repo = new SailRepository(sail);
		repo.initialize();
		vf = repo.getValueFactory();
		con = repo.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
		FileUtil.deltree(dir);
	}

	public void testFirstWord() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base"));
		assertTrue(qry.evaluate());
	}

	public void testLastWord() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("ball"));
		assertTrue(qry.evaluate());
	}

	public void testFullLabel() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base ball"));
		assertTrue(qry.evaluate());
	}

	public void testTooLong() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base ball bat"));
		assertFalse(qry.evaluate());
	}

	public void testRestart() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		con.close();
		repo.shutDown();
		Sail sail = new KeywordSail(new MemoryStore(dir));
		repo = new SailRepository(sail);
		repo.initialize();
		vf = repo.getValueFactory();
		con = repo.getConnection();
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base ball"));
		assertTrue(qry.evaluate());
	}

	public void testNewProperty() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDF.VALUE,
				vf.createLiteral("base ball"));
		con.close();
		repo.shutDown();
		KeywordSail sail = new KeywordSail(new MemoryStore(dir));
		sail.setKeywordProperties(Collections.singleton(RDF.VALUE));
		repo = new SailRepository(sail);
		repo.initialize();
		vf = repo.getValueFactory();
		con = repo.getConnection();
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdf:value ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base ball"));
		assertTrue(qry.evaluate());
	}

	public void testChangeGraph() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		URI phone = vf.createURI("http://www.openrdf.org/rdf/2011/keyword#phone");
		assertTrue(con.hasStatement(vf.createURI("urn:test:ball"), phone, null, true, new Resource[]{null}));
		con.close();
		repo.shutDown();
		KeywordSail sail = new KeywordSail(new MemoryStore(dir));
		sail.setPhoneGraph(vf.createURI("urn:test:keywords"));
		repo = new SailRepository(sail);
		repo.initialize();
		vf = repo.getValueFactory();
		con = repo.getConnection();
		assertTrue(con.hasStatement(vf.createURI("urn:test:ball"), phone, null, true, new Resource[]{vf.createURI("urn:test:keywords")}));
		assertFalse(con.hasStatement(vf.createURI("urn:test:ball"), phone, null, true, new Resource[]{null}));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label\n"
			+ "GRAPH <urn:test:keywords> { ?resource keyword:phone ?soundex }\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base ball"));
		assertTrue(qry.evaluate());
	}

	public void testNewStore() throws Exception {
		tearDown();
		Sail sail = new MemoryStore(dir);
		repo = new SailRepository(sail);
		repo.initialize();
		vf = repo.getValueFactory();
		con = repo.getConnection();
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL, vf.createLiteral("base ball"));
		URI phone = vf.createURI("http://www.openrdf.org/rdf/2011/keyword#phone");
		assertFalse(con.hasStatement(vf.createURI("urn:test:ball"), phone, null, true, new Resource[]{null}));
		con.close();
		repo.shutDown();
		setUp();
		assertTrue(con.hasStatement(vf.createURI("urn:test:ball"), phone, null, true, new Resource[]{null}));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base ball"));
		assertTrue(qry.evaluate());
	}
}
