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
package org.callimachusproject.repository.auditing;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.iteration.CloseableIteration;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;

import junit.framework.TestCase;

import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepository;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.sail.auditing.AuditingSail;
import org.openrdf.OpenRDFException;
import org.openrdf.model.BNode;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

public class AuditingPurgeTest extends TestCase {
	private static final String PREFIX = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX prov:<http://www.w3.org/ns/prov#>\n" +
			"PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX audit:<http://www.openrdf.org/rdf/2012/auditing#>";
	/** Added by ActivityFactory on first change */
	public static final URI BUNDLE = new URIImpl("http://www.w3.org/ns/prov#Bundle");
	/** Added after activity is finished */
	public static final URI RECENT = new URIImpl("http://www.openrdf.org/rdf/2012/auditing#RecentBundle");
	/** Added after no other triples present and after all other triples removed from activity graph */
	public static final URI OBSOLETE = new URIImpl("http://www.openrdf.org/rdf/2012/auditing#ObsoleteBundle");
	/** Added by this ActivityFactory after data is committed */
	public static final URI ENDED_AT = new URIImpl("http://www.w3.org/ns/prov#endedAtTime");
	/**
	 * Added when entity triple is inserted, replaced after entity triple is
	 * deleted, added/replaced after triple is inserted
	 */
	public static final URI GENERATED_BY = new URIImpl("http://www.w3.org/ns/prov#wasGeneratedBy");
	/** Added when entity triple is removed, after entity triple is inserted, and after triple is inserted. */  
	public static final URI GENERATED = new URIImpl("http://www.w3.org/ns/prov#generated");
	/** Added when entity triple is removed, after entity triple is inserted, and after triple is inserted. */ 
	public static final URI SPECIALIZATION_OF = new URIImpl("http://www.w3.org/ns/prov#specializationOf");
	/**
	 * Added by trigger when triple is removed from a graph. Relates the
	 * activity graph to the graph of the triple deleted/inserted.
	 */
	public static final URI INFLUENCED_BY = new URIImpl("http://www.w3.org/ns/prov#wasInfluencedBy");
	/** Added by trigger when prov:wasGeneratedBy triple is removed from a graph */
	public static final URI REVISION_OF = new URIImpl("http://www.w3.org/ns/prov#wasRevisionOf");
	/** Added by trigger when entity triple is reified */
	public static final URI QUALIFIED_REVISION = new URIImpl("http://www.w3.org/ns/prov#qualifiedRevision");
	/** Added by trigger when entity triple is reified */
	public static final URI ENTITY = new URIImpl("http://www.w3.org/ns/prov#entity");
	/** Added by trigger when entity triple is reified */
	public static final URI WITHOUT = new URIImpl("http://www.openrdf.org/rdf/2012/auditing#without");
	ValueFactory vf = ValueFactoryImpl.getInstance();
	private String NS = "http://example.com/";
	private URI carmichael = vf.createURI(NS, "carmichael");
	private URI harris = vf.createURI(NS, "harris");
	private URI jackson = vf.createURI(NS, "jackson");
	private URI johnston = vf.createURI(NS, "johnston");
	private URI lismer = vf.createURI(NS, "lismer");
	private URI macDonald = vf.createURI(NS, "macDonald");
	private URI varley = vf.createURI(NS, "varley");
	private URI thomson = vf.createURI(NS, "thomson");
	private URI knows = vf.createURI("http://xmlns.com/foaf/0.1/knows");
	private URI graph = vf.createURI(NS, "graph");
	private URI set = vf.createURI(NS, "set");
	URI lastProvActivity;
	URI lastActivityGraph;
	private AuditingRepositoryConnection con;
	private AuditingRepository repo;

	private AuditingRepositoryConnection reopen(AuditingRepository repo,
			AuditingRepositoryConnection con) throws Exception {
		con = commit(repo, con);
		begin(con);
		return con;
	}

	private void begin(AuditingRepositoryConnection conn)
			throws RepositoryException {
		conn.begin();
	}

	private AuditingRepositoryConnection commit(AuditingRepository repo,AuditingRepositoryConnection conn)
			throws Exception {
		conn.commit();
		conn.close();
		return repo.getConnection();
	}

	private boolean ask(String... string) throws OpenRDFException {
		StringBuilder sb = new StringBuilder();
		sb.append("BASE <").append(NS).append(">\n");
		sb.append(PREFIX).append("ASK {\n");
		for (String str : string) {
			sb.append(str).append("\n");
		}
		sb.append("}");
		return con.prepareBooleanQuery(SPARQL, sb.toString()).evaluate();
	}

	private void dump(OutputStream out) throws RepositoryException,
			RDFHandlerException {
		TriGWriter handler = new TriGWriter(System.out);
		handler.startRDF();

		// Export namespace information
		CloseableIteration<? extends Namespace, RepositoryException> nsIter = con.getNamespaces();
		try {
			while (nsIter.hasNext()) {
				Namespace ns = nsIter.next();
				handler.handleNamespace(ns.getPrefix(), ns.getName());
			}
		} finally {
			nsIter.close();
		}

		// Export statements

		for (Resource ctx : asList(con.getContextIDs())) {
			for (Resource subj : addAll(
					con.getStatements(null, null, null, false, ctx),
					new TreeModel()).subjects()) {
				RepositoryResult<Statement> stIter = con.getStatements(
								subj, null, null, false, ctx);

				try {
					while (stIter.hasNext()) {
						handler.handleStatement(stIter.next());
					}
				} finally {
					stIter.close();
				}
			}
		}

		handler.endRDF();
	}

	public void setUp() throws Exception {
		Sail sail = new MemoryStore();
		sail = new AuditingSail(sail);
		Repository r = new SailRepository(sail);
		repo = new AuditingRepository(r);
		repo.setPurgeAfter(DatatypeFactory.newInstance().newDuration("PT0S"));
		repo.initialize();
		vf = repo.getValueFactory();
		final ActivityFactory delegate = repo.getActivityFactory();
		repo.setActivityFactory(new ActivityFactory() {

			public URI createActivityURI(URI bundle, ValueFactory vf) {
				URI prov = lastProvActivity = delegate.createActivityURI(bundle, vf);
				String uri = prov.stringValue();
				lastActivityGraph = vf.createURI(uri.substring(0, uri.indexOf('#')));
				return prov;
			}

			public void activityStarted(URI activity,
					URI bundle, RepositoryConnection con) throws RepositoryException {
				delegate.activityStarted(activity, bundle, con);
				con.add(bundle, RDF.TYPE, BUNDLE, bundle);
			}

			public void activityEnded(URI activity,
					URI bundle, RepositoryConnection con) throws RepositoryException {
				con.remove(activity, ENDED_AT, null, bundle);
				delegate.activityEnded(activity, bundle, con);
			}
		});
		con = repo.getConnection();
	}

	@Override
	protected void runTest() throws Throwable {
		try {
			super.runTest();
		} catch (Throwable t) {
			System.err.println("=== " + getName() + " Faild ===");
			dump(System.out);
			throw t;
		}
	}

	public void tearDown() throws Exception {
		con.rollback();
		con.close();
		repo.shutDown();
	}

	public void testAdd() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER strstarts(str(?provenance1), concat(str(?activity1), '#'))",
				"}"));
	}

	public void testAddUncommitted() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertFalse(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER NOT EXISTS { ?activity1 prov:endedAtTime ?ended1 }",
				"}"));
		con = commit(repo, con);
	}

	public void testSetUncommitted() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.remove(carmichael, knows, null);
		con.add(carmichael, knows, harris);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertFalse(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER NOT EXISTS { ?activity1 prov:endedAtTime ?ended1 }",
				"}"));
		con = commit(repo, con);
	}

	public void testAddMany() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.add(johnston, knows, carmichael);
		con = reopen(repo, con);
		con.add(carmichael, knows, jackson);
		con.add(harris, knows, jackson);
		con.add(jackson, knows, johnston);
		con = reopen(repo, con);
		con.add(carmichael, knows, lismer);
		con.add(harris, knows, macDonald);
		con.add(jackson, knows, varley);
		con.add(johnston, knows, lismer);
		con.add(lismer, knows, macDonald);
		con.add(macDonald, knows, varley);
		con.add(varley, knows, thomson);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(3, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertEquals(0, asList(con.getStatements(null, RDF.TYPE, RECENT, false)).size());
		assertTrue(ask("FILTER NOT EXISTS {\n"
				+ "    ?activity prov:generated [prov:specializationOf ?self]\n"
				+ "    FILTER (strstarts(?self,?activity))\n" + "}\n"));
		assertTrue(ask("FILTER NOT EXISTS {\n"
				+ "    ?activity prov:generated ?generated\n"
				+ "    FILTER (!strstarts(?generated,?activity))\n" + "}\n"));
		assertFalse(ask("?activity prov:wasInfluencedBy ?activity"));
		assertTrue(ask(
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1, ?johnston1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ?johnston1 prov:specializationOf <johnston> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"    <johnston> foaf:knows <carmichael> .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER (str(?johnston1) = concat(str(?activity1), '#!', str(<johnston>)))",
				"    FILTER NOT EXISTS { ?activity1 a audit:ObsoleteBundle }",
				"    FILTER NOT EXISTS { ?e prov:wasGeneratedBy ?p FILTER (?e != ?activity1) }",
				"}",
				"GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2, ?harris2, ?jackson2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ?harris2 prov:specializationOf <harris> .",
				"    ?jackson2 prov:specializationOf <jackson> .",
				"    ",
				"    <carmichael> foaf:knows <jackson> .",
				"    <harris> foaf:knows <jackson> .",
				"    <jackson> foaf:knows <johnston> .",
				"    ",
				"    FILTER (str(?carmichael2) = concat(str(?activity2), '#!', str(<carmichael>)))",
				"    FILTER (str(?harris2) = concat(str(?activity2), '#!', str(<harris>)))",
				"    FILTER (str(?jackson2) = concat(str(?activity2), '#!', str(<jackson>)))",
				"    FILTER NOT EXISTS { ?activity2 a audit:ObsoleteBundle }",
				"    FILTER NOT EXISTS { ?e prov:wasGeneratedBy ?p FILTER (?e != ?activity2) }",
				"}",
				"GRAPH ?activity3 {",
				"    ?activity3 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1, ?activity2 ;",
				"        prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    ?provenance3 prov:endedAtTime ?ended3 ;",
				"        prov:generated ?carmichael3, ?harris3, ?jackson3, ?johnston3, ?lismer3, ?macDonald3, ?varley3 .",
				"    ",
				"    ?carmichael3 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael2 .",
				"    ?harris3 prov:specializationOf <harris> .",
				"    ?jackson3 prov:specializationOf <jackson> .",
				"    ?johnston3 prov:specializationOf <johnston> ;",
				"        prov:wasRevisionOf ?johnston1 .",
				"    ?lismer3 prov:specializationOf <lismer> .",
				"    ?macDonald3 prov:specializationOf <macDonald> .",
				"    ?varley3 prov:specializationOf <varley> .",
				"    ",
				"    <carmichael> foaf:knows <lismer> ; prov:wasGeneratedBy ?provenance3 .",
				"    <harris> foaf:knows <macDonald> ; prov:wasGeneratedBy ?provenance3 .",
				"    <jackson> foaf:knows <varley> ; prov:wasGeneratedBy ?provenance3 .",
				"    <johnston> foaf:knows <lismer> ; prov:wasGeneratedBy ?provenance3 .",
				"    <lismer> foaf:knows <macDonald> ; prov:wasGeneratedBy ?provenance3 .",
				"    <macDonald> foaf:knows <varley> ; prov:wasGeneratedBy ?provenance3 .",
				"    <varley> foaf:knows <thomson> ; prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    FILTER (str(?johnston3) = concat(str(?activity3), '#!', str(<johnston>)))",
				"    FILTER (str(?lismer3) = concat(str(?activity3), '#!', str(<lismer>)))",
				"    FILTER NOT EXISTS { ?activity3 a audit:ObsoleteBundle }",
				"}"));
	}

	public void testAddManyDefaultGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		repo.setInsertContext(graph);
		con = reopen(repo, con);
		con.add(carmichael, knows, harris);
		con.add(johnston, knows, carmichael);
		con = reopen(repo, con);
		con.add(carmichael, knows, jackson);
		con.add(harris, knows, jackson);
		con.add(jackson, knows, johnston);
		con = reopen(repo, con);
		con.add(carmichael, knows, lismer);
		con.add(harris, knows, macDonald);
		con.add(jackson, knows, varley);
		con.add(johnston, knows, lismer);
		con.add(lismer, knows, macDonald);
		con.add(macDonald, knows, varley);
		con.add(varley, knows, thomson);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertEquals(0, asList(con.getStatements(null, RDF.TYPE, RECENT, false)).size());
		assertTrue(ask("FILTER NOT EXISTS {\n"
				+ "    ?activity prov:generated [prov:specializationOf ?self]\n"
				+ "    FILTER (strstarts(?self,?activity))\n" + "}\n"));
		assertTrue(ask("FILTER NOT EXISTS {\n"
				+ "    ?activity prov:generated ?generated\n"
				+ "    FILTER (!strstarts(?generated,?activity))\n" + "}\n"));
		assertFalse(ask("?activity prov:wasInfluencedBy ?activity"));
		assertTrue(ask(
				"GRAPH <graph> {",
				"    <graph> a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael3, ?harris1, ?jackson1, ?johnston1, ?lismer1, ?macDonald1, ?varley1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ?johnston1 prov:specializationOf <johnston> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"    <johnston> foaf:knows <carmichael> .",
				"    ",
				"    ?harris1 prov:specializationOf <harris> .",
				"    ?jackson1 prov:specializationOf <jackson> .",
				"    ",
				"    <carmichael> foaf:knows <jackson> .",
				"    <harris> foaf:knows <jackson> .",
				"    <jackson> foaf:knows <johnston> .",
				"    ",
				"    ?harris1 prov:specializationOf <harris> .",
				"    ?jackson1 prov:specializationOf <jackson> .",
				"    ?lismer1 prov:specializationOf <lismer> .",
				"    ?macDonald1 prov:specializationOf <macDonald> .",
				"    ?varley1 prov:specializationOf <varley> .",
				"    ",
				"    <carmichael> foaf:knows <lismer> ; prov:wasGeneratedBy ?provenance1 .",
				"    <harris> foaf:knows <macDonald> ; prov:wasGeneratedBy ?provenance1 .",
				"    <jackson> foaf:knows <varley> ; prov:wasGeneratedBy ?provenance1 .",
				"    <johnston> foaf:knows <lismer> ; prov:wasGeneratedBy ?provenance1 .",
				"    <lismer> foaf:knows <macDonald> ; prov:wasGeneratedBy ?provenance1 .",
				"    <macDonald> foaf:knows <varley> ; prov:wasGeneratedBy ?provenance1 .",
				"    <varley> foaf:knows <thomson> ; prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?johnston1) = concat(str(<graph>), '#!', str(<johnston>)))",
				"    FILTER (str(?lismer1) = concat(str(<graph>), '#!', str(<lismer>)))",
				"    FILTER NOT EXISTS { <graph> a audit:ObsoleteBundle }",
				"    FILTER NOT EXISTS { <graph> prov:wasInfluencedBy ?other }",
				"}"));
	}

	public void testRemove() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance2 ;",
				"        prov:wasInfluencedBy ?activity1 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    FILTER (str(?carmichael2) = concat(str(?activity2), '#!', str(<carmichael>)))",
				"    FILTER NOT EXISTS { ?activity1 prov:wasGeneratedBy ?provenance2 }",
				"    FILTER NOT EXISTS { ?activity2 a audit:ObsoleteBundle }",
				"}"));
	}

	public void testRemoveSome() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.add(carmichael, knows, jackson);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <jackson> .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER NOT EXISTS { <carmichael> prov:wasGeneratedBy ?activity1 }",
				"    FILTER NOT EXISTS { <carmichael> prov:wasGeneratedBy ?provenance1 }",
				"}",
				"GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance2 ;",
				"        prov:wasInfluencedBy ?activity1 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    FILTER (str(?carmichael2) = concat(str(?activity2), '#!', str(<carmichael>)))",
				"    FILTER NOT EXISTS { ?activity2 a audit:ObsoleteBundle }",
				"}"));
	}

	public void testRemoveNil() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.remove(carmichael, knows, harris);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"}"));
	}

	public void testRemoveDefaultGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		repo.setInsertContext(graph);
		con = reopen(repo, con);
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH <graph> {",
				"    <graph> a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(<graph>), '#!', str(<carmichael>)))",
				"    FILTER NOT EXISTS { <graph> a audit:ObsoleteBundle }",
				"}"));
	}

	public void testRemoveMany() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.add(harris, knows, jackson);
		con.add(jackson, knows, johnston);
		con.add(johnston, knows, lismer);
		con = reopen(repo, con);
		con.remove((Resource)null, knows, null);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertEquals(0, asList(con.getStatements(null, RDF.TYPE, RECENT, false)).size());
		assertTrue(ask("GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1, ?harris1, ?jackson1, ?johnston1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ?harris1 prov:specializationOf <harris> .",
				"    ?jackson1 prov:specializationOf <jackson> .",
				"    ?johnston1 prov:specializationOf <johnston> .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    <harris> prov:wasGeneratedBy ?provenance1 .",
				"    <jackson> prov:wasGeneratedBy ?provenance1 .",
				"    <johnston> prov:wasGeneratedBy ?provenance1 .",
				"    FILTER NOT EXISTS { ?activity1 a audit:ObsoleteBundle }",
				"}"));
	}

	public void testRemoveAdd() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(carmichael, knows, jackson);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity3 {",
				"    ?activity3 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity2 ;",
				"        prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    ?provenance3 prov:endedAtTime ?ended3 ;",
				"        prov:generated ?carmichael3 .",
				"    ",
				"    ?carmichael3 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael2 .",
				"    <carmichael> foaf:knows <jackson> ; prov:wasGeneratedBy ?provenance3 .",
				"}"));
	}

	public void testReplace() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con.add(carmichael, knows, jackson);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(ask("?activity prov:wasInfluencedBy ?activity"));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> .",
				"    <carmichael> foaf:knows <jackson> ; prov:wasGeneratedBy ?provenance2 .",
				"}"));
	}

	public void testRemoveEach() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		RepositoryResult<Statement> stmts = con.getStatements(carmichael, null, null, false);
		while (stmts.hasNext()) {
			con.remove(stmts.next());
		}
		stmts.close();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(ask("<carmichael> prov:wasGeneratedBy ?provenance"));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity1 prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2, ?activity12 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> .",
				"    ?activity12 prov:specializationOf ?activity1 .",
				"    FILTER NOT EXISTS { ?activity12 prov:wasRevisionOf ?revision }",
				"}"));
	}

	public void testRemoveRevision() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, GENERATED_BY, null);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(ask("<carmichael> prov:wasGeneratedBy ?provenance"));
		assertTrue(ask("GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"}"));
	}

	public void testRemoveLastRevision() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		RepositoryResult<Statement> stmts = con.getStatements(carmichael, GENERATED_BY, null, false);
		Value revision = stmts.next().getObject();
		stmts.close();
		con.remove(carmichael, GENERATED_BY, revision);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(ask("<carmichael> prov:wasGeneratedBy ?provenance"));
		assertTrue(ask("GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"}"));
	}

	public void testDoubleTouchRevision() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, GENERATED_BY, null);
		con.add(carmichael, GENERATED_BY, lastProvActivity);
		con = reopen(repo, con);
		con.remove(carmichael, GENERATED_BY, null);
		con.add(carmichael, GENERATED_BY, lastProvActivity);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("<carmichael> prov:wasGeneratedBy ?provenance"));
		assertTrue(ask("GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"}",
				"GRAPH ?activity3 {",
				"    ?activity3 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity2 ;",
				"        prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    ?provenance3 prov:endedAtTime ?ended3 ;",
				"        prov:generated ?carmichael3 .",
				"    ",
				"    ?carmichael3 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael2 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance3 .",
				"}"));
	}

	public void testUpgrade() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(carmichael, knows, jackson);
		con = reopen(repo, con);
		con.remove(carmichael, null, null);
		con.add(carmichael, knows, johnston);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, johnston, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(ask("?activity prov:wasInfluencedBy ?activity"));
		assertTrue(ask("GRAPH ?activity3 {",
				"    ?activity3 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1, ?activity2 ;",
				"        prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    ?provenance3 prov:endedAtTime ?ended3 ;",
				"        prov:generated ?carmichael3 .",
				"    ",
				"    ?carmichael3 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael2 .",
				"    ",
				"    <carmichael> foaf:knows <johnston> ; prov:wasGeneratedBy ?provenance3 .",
				"}"));
	}

	public void testClear() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, null, null);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(0, asList(con.getContextIDs()).size());
		assertFalse(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertFalse(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(ask("<carmichael> prov:wasGeneratedBy ?provenance"));
	}

	public void testInsertData() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"}"));
	}

	public void testDeleteData() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE DATA { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    FILTER (str(?carmichael2) = concat(str(?activity2), '#!', str(<carmichael>)))",
				"    FILTER NOT EXISTS { ?activity2 a audit:ObsoleteBundle }",
				"}"));
	}

	public void testDelete() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE { <carmichael> ?p ?o }\n" +
				"WHERE { <carmichael> ?p ?o } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(0, asList(con.getContextIDs()).size());
		assertFalse(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertFalse(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(ask("<carmichael> prov:wasGeneratedBy ?provenance"));
	}

	public void testModify() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE { <carmichael> ?p ?o }\n" +
				"INSERT { <carmichael> <http://xmlns.com/foaf/0.1/knows> <jackson> }\n" +
				"WHERE { <carmichael> ?p ?o } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(ask("?activity prov:wasInfluencedBy ?activity"));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> .",
				"    <carmichael> foaf:knows <jackson> ; prov:wasGeneratedBy ?provenance2 .",
				"}"));
	}

	public void testRollback() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE { <carmichael> ?p ?o }\n" +
				"INSERT { <carmichael> <http://xmlns.com/foaf/0.1/knows> <jackson> }\n" +
				"WHERE { <carmichael> ?p ?o } ", "http://example.com/").execute();
		con.rollback();
		con.close();
		con = repo.getConnection();
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"}"));
	}

	public void testAutoCommit() throws Exception {
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.close();
		con = repo.getConnection();
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <harris> .",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"}"));
	}

	public void testAddGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris, graph);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(2, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH <graph> {",
				"    <carmichael> foaf:knows <harris>",
				"}",
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1, ?graph1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ?graph1 prov:specializationOf <graph> .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    <graph> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?graph1) = concat(str(?activity1), '#!', str(<graph>)))",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER strstarts(str(?provenance1), concat(str(?activity1), '#'))",
				"}"));
	}

	public void testAddDefaultGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.setInsertContext(graph);
		con.add(carmichael, knows, harris);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(Arrays.asList(graph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH <graph> {",
				"    <carmichael> foaf:knows <harris> .",
				"    <graph> a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1, ?graph1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(<graph>), '#!', str(<carmichael>)))",
				"    FILTER strstarts(str(?provenance1), concat(str(<graph>), '#'))",
				"}"));
	}

	public void testAddHashGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		String other = con.getInsertContext() + "#other-graph";
		con.add(carmichael, knows, harris, vf.createURI(other));
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(2, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH ?other {",
				"    <carmichael> foaf:knows <harris>",
				"}",
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1, ?other .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    ?other prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?other) = concat(str(?activity1), '#other-graph'))",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER strstarts(str(?provenance1), concat(str(?activity1), '#'))",
				"    FILTER NOT EXISTS { ?something prov:specializationOf ?other }",
				"}"));
	}

	public void testInsertDataGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { GRAPH <graph> { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } }", "http://example.com/").execute();
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(2, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH <graph> {",
				"    <carmichael> foaf:knows <harris>",
				"}",
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1, ?graph1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ?graph1 prov:specializationOf <graph> .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    <graph> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER strstarts(str(?provenance1), concat(str(?activity1), '#'))",
				"}"));
	}

	public void testInsertWithGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.prepareUpdate(QueryLanguage.SPARQL, "WITH <graph> INSERT { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } WHERE {}", "http://example.com/").execute();
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(2, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask(
				"GRAPH <graph> {",
				"    <carmichael> foaf:knows <harris>",
				"}",
				"GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1, ?graph1 .",
				"    ",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ?graph1 prov:specializationOf <graph> .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance1 .",
				"    <graph> prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER strstarts(str(?provenance1), concat(str(?activity1), '#'))",
				"}"));
	}

	public void testDeleteDataGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris, graph);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE DATA { GRAPH <graph> { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy <graph>, ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2, ?graph2 .",
				"    ",
				"    ?graph2 prov:specializationOf <graph> ;",
				"        prov:wasRevisionOf ?graph1 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance2 .",
				"    <graph> prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER (str(?carmichael2) = concat(str(?activity2), '#!', str(<carmichael>)))",
				"    FILTER NOT EXISTS { ?activity2 a audit:ObsoleteBundle }",
				"}"));
	}

	public void testDeleteHashGraph() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		String other = con.getInsertContext() + "#other-graph";
		con.add(carmichael, knows, harris, vf.createURI(other));
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE DATA { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> }", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?other, ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    FILTER (str(?carmichael1) = concat(str(?activity1), '#!', str(<carmichael>)))",
				"    FILTER (str(?carmichael2) = concat(str(?activity2), '#!', str(<carmichael>)))",
				"    FILTER NOT EXISTS { ?activity2 a audit:ObsoleteBundle }",
				"}"));
	}

	public void testDeleteWhere() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.add(harris, knows, jackson);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, PREFIX + "DELETE { ?friend foaf:knows ?foaf } WHERE { <carmichael> foaf:knows ?friend . ?friend foaf:knows ?foaf } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(harris, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity1 {",
				"    ?activity1 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance1 .",
				"    ",
				"    ?provenance1 prov:endedAtTime ?ended1 ;",
				"        prov:generated ?carmichael1, ?harris1 .",
				"    ",
				"    ?harris1 prov:specializationOf <harris> .",
				"    ?carmichael1 prov:specializationOf <carmichael> .",
				"    ",
				"    <carmichael> foaf:knows <harris> ; prov:wasGeneratedBy ?provenance1 .",
				"    <harris> prov:wasGeneratedBy ?provenance1 .",
				"}"));
	}

	public void testDeleteConnected() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		BNode node = vf.createBNode();
		con.add(carmichael, knows, node);
		con.add(node, knows, jackson);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, PREFIX + "DELETE { <carmichael> foaf:knows ?friend . ?friend foaf:knows ?foaf } WHERE { <carmichael> foaf:knows ?friend . ?friend foaf:knows ?foaf } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(harris, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance2 .",
				"}"));
	}

	public void testDeleteLink() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		BNode node = vf.createBNode();
		con.add(carmichael, knows, node);
		con.add(node, knows, jackson);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, PREFIX + "DELETE { <carmichael> foaf:knows ?friend } WHERE { <carmichael> foaf:knows ?friend } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(harris, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(1, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertFalse(con.hasStatement(null, null, jackson, false));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1 ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?carmichael2 .",
				"    ",
				"    ?carmichael2 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance2 .",
				"}"));
	}

	public void testDeleteChain() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(harris, knows, jackson);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, PREFIX + "DELETE { <carmichael> foaf:knows ?friend . ?friend foaf:knows ?foaf } WHERE { <carmichael> foaf:knows ?friend . ?friend foaf:knows ?foaf } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(harris, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?harris2 .",
				"    ",
				"    ?harris2 prov:specializationOf <harris> .",
				"    ",
				"    <harris> prov:wasGeneratedBy ?provenance2 .",
				"}",
				"GRAPH ?activity3 {",
				"    ?activity3 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1, ?activity2 ;",
				"        prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    ?provenance3 prov:endedAtTime ?ended3 ;",
				"        prov:generated ?carmichael3 .",
				"    ",
				"    ?carmichael3 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    FILTER NOT EXISTS {",
				"    ?triple2 rdf:subject <harris> ;",
				"        rdf:predicate foaf:knows ;",
				"        rdf:object <jackson> .",
				"    }",
				"}"));
	}

	public void testDeletePair() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(harris, knows, jackson);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, PREFIX + "DELETE { <carmichael> foaf:knows ?friend . <harris> foaf:knows ?foaf } WHERE { <carmichael> foaf:knows ?friend . <harris> foaf:knows ?foaf } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(harris, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(Arrays.asList(lastActivityGraph), asList(con.getContextIDs()));
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity3 {",
				"    ?activity3 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1, ?activity2 ;",
				"        prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    ?provenance3 prov:endedAtTime ?ended3 ;",
				"        prov:generated ?carmichael3, ?harris3 .",
				"    ",
				"    ?carmichael3 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    ?harris3 prov:specializationOf <harris> ;",
				"        prov:wasRevisionOf ?harris2 .",
				"    ",
				"    <carmichael> prov:wasGeneratedBy ?provenance3 .",
				"    <harris> prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    FILTER NOT EXISTS {",
				"    ?triple1 rdf:subject <carmichael> ;",
				"        rdf:predicate foaf:knows ;",
				"        rdf:object <harris> .",
				"    }",
				"    ",
				"    FILTER NOT EXISTS {",
				"    ?triple2 rdf:subject <harris> ;",
				"        rdf:predicate foaf:knows ;",
				"        rdf:object <jackson> .",
				"    }",
				"}"));
	}

	public void testDeleteGraphChain() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris, graph);
		con = reopen(repo, con);
		con.add(harris, knows, jackson, set);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, PREFIX + "DELETE { GRAPH <graph> { <carmichael> foaf:knows ?friend } GRAPH <set> { ?friend foaf:knows ?foaf } } WHERE { <carmichael> foaf:knows ?friend . ?friend foaf:knows ?foaf } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(harris, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, asList(con.getContextIDs()).size());
		assertTrue(con.hasStatement(null, RDF.TYPE, BUNDLE, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFLUENCED_BY, null, false));
		assertFalse(con.hasStatement(null, WITHOUT, null, false));
		assertTrue(ask("GRAPH ?activity2 {",
				"    ?activity2 a prov:Bundle ;",
				"        prov:wasGeneratedBy ?provenance2 .",
				"    ",
				"    ?provenance2 prov:endedAtTime ?ended2 ;",
				"        prov:generated ?harris2, ?set2 .",
				"    ",
				"    ?set2 prov:specializationOf <set> .",
				"    ?harris2 prov:specializationOf <harris> .",
				"    ",
				"    <harris> prov:wasGeneratedBy ?provenance2 .",
				"}",
				"GRAPH ?activity3 {",
				"    ?activity3 a prov:Bundle ;",
				"        prov:wasInfluencedBy ?activity1, ?activity2, <graph>, <set> ;",
				"        prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    ?provenance3 prov:endedAtTime ?ended3 ;",
				"        prov:generated ?carmichael3, ?graph3, ?set3 .",
				"    ",
				"    ?graph3 prov:specializationOf <graph> ;",
				"        prov:wasRevisionOf ?graph1 .",
				"    ?set3 prov:specializationOf <set> ;",
				"        prov:wasRevisionOf ?set2 .",
				"    ?carmichael3 prov:specializationOf <carmichael> ;",
				"        prov:wasRevisionOf ?carmichael1 .",
				"    ",
				"    <graph> prov:wasGeneratedBy ?provenance3 .",
				"    <set> prov:wasGeneratedBy ?provenance3 .",
				"    <carmichael> prov:wasGeneratedBy ?provenance3 .",
				"    ",
				"    FILTER NOT EXISTS {",
				"    ?triple2 rdf:subject <harris> ;",
				"        rdf:predicate foaf:knows ;",
				"        rdf:object <jackson> .",
				"    }",
				"}"));
	}

	/**
	 * Get a List containing all elements obtained from the specified Iteration.
	 * 
	 * @param iter
	 *            the Iteration to get the elements from
	 * @return a List containing all elements obtained from the specified
	 *         Iteration.
	 * @throws RepositoryException
	 */
	private static <E> List<E> asList(RepositoryResult<E> iter)
			throws RepositoryException {
		List<E> result = new ArrayList<E>();
		addAll(iter, result);
		return result;
	}

	/**
	 * Adds all elements from the supplied Iteration to the specified
	 * collection. If the supplied Iteration is an instance of
	 * {@link CloseableIteration} it is automatically closed after consumption.
	 * 
	 * @param iter
	 *            An Iteration containing elements to add to the container. If
	 *            the Iteration is an instance of {@link CloseableIteration} it
	 *            is automatically closed after consumption.
	 * @param collection
	 *            The collection to add the elements to.
	 * @return The <tt>collection</tt> object that was supplied to this method.
	 * @throws RepositoryException
	 */
	private static <E, C extends Collection<E>> C addAll(
			RepositoryResult<? extends E> iter, C collection)
			throws RepositoryException {
		try {
			while (iter.hasNext()) {
				collection.add(iter.next());
			}
		} finally {
			iter.close();
		}

		return collection;
	}
}
