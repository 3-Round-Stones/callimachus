package org.callimachusproject.server;

import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.NamedGraphSupport;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;

public class NamedGraphTest extends MetadataServerTestCase {

	public void setUp() throws Exception {
		config.addBehaviour(PUTSupport.class);
		config.addBehaviour(NamedGraphSupport.class);
		super.setUp();
	}

	public void testPUT() throws Exception {
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Model result = graph.accept("application/rdf+xml").get(Model.class);
		assertEquals(model, result);
	}

	public void testPUTNamespace() throws Exception {
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.setNamespace("test", "urn:test:");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Model result = graph.accept("application/rdf+xml").get(Model.class);
		assertEquals("urn:test:", result.getNamespaces().get("test"));
	}

	public void testGZip() throws Exception {
		WebResource client = Client.create().resource("http://" + host);
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph");
		graph.addFilter(new GZIPContentEncodingFilter(true));
		graph.type("application/x-turtle").put(model);
		Model result = graph.accept("application/rdf+xml").get(Model.class);
		assertEquals(model, result);
	}
}
