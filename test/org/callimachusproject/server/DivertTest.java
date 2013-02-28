package org.callimachusproject.server;

import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.DescribeSupport;
import org.openrdf.model.Model;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectConnection;

public class DivertTest extends MetadataServerTestCase {

	private ObjectConnection con;

	public void setUp() throws Exception {
		config.addBehaviour(DescribeSupport.class, RDFS.RESOURCE);
		super.setUp();
		String prefix = client.path("/absolute;").getURI().toASCIIString();
		server.setIndirectIdentificationPrefix(new String[] { prefix });
		con = repository.getConnection();
	}

	@Override
	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public void testRequest() throws Exception {
		URIImpl subj = new URIImpl("urn:test:annotation");
		con.add(subj, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
		Model model = client.path("/absolute;urn:test:annotation").queryParam("describe", "").get(Model.class);
		assertTrue(model.contains(subj, RDF.TYPE, OWL.ANNOTATIONPROPERTY));
	}

	public void testResponse() throws Exception {
		URIImpl subj = new URIImpl("urn:test:annotation");
		con.add(subj, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
		Model model = client.path("/absolute;urn:test:annotation").get(Model.class);
		assertTrue(model.contains(subj, RDF.TYPE, OWL.ANNOTATIONPROPERTY));
	}
}
