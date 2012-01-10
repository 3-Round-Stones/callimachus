package org.callimachusproject.server;

import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.NamedGraphSupport;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.callimachusproject.server.behaviours.TextFile;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectConnection;

import com.sun.jersey.api.client.WebResource;

public class DynamicOperationTest extends MetadataServerTestCase {

	public void setUp() throws Exception {
		config.addBehaviour(TextFile.class, "urn:mimetype:text/plain");
		config.addBehaviour(PUTSupport.class);
		config.addBehaviour(NamedGraphSupport.class);
		config.setCompileRepository(true);
		super.setUp();
	}

	public void testDynamicOperation() throws Exception {
		URI ICON = vf.createURI("urn:test:icon");
		String META = "http://www.openrdf.org/rdf/2011/messaging#";
		URI OPERATION = vf.createURI(META + "query");
		URI EXPECT = vf.createURI(META + "expect");
		Model rdf = new LinkedHashModel();
		rdf.add(ICON, RDF.TYPE, RDF.PROPERTY);
		rdf.add(ICON, RDFS.DOMAIN, RDFS.RESOURCE);
		rdf.add(ICON, OPERATION, vf.createLiteral("icon"));
		rdf.add(ICON, EXPECT, vf.createLiteral("303-see-other"));
		WebResource resource = client.path("/resource");
		WebResource icon = client.path("/icon");
		String uri = resource.getURI().toASCIIString();
		String icon_uri = icon.getURI().toASCIIString();
		rdf.add(vf.createURI(uri), ICON, vf.createURI(icon_uri));
		client.path("/schema.rdf").type("application/rdf+xml").put(rdf);
		ObjectConnection con = repository.getConnection();
		con.recompileSchemaOnClose();
		con.close();
		icon.type("text/plain").put("my icon");
		WebResource resource_icon = resource.queryParam("icon", "");
		assertEquals("my icon", resource_icon.get(String.class));
	}
}
