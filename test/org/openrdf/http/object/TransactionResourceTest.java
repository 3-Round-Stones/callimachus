package org.openrdf.http.object;

import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.behaviours.NamedGraphSupport;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import com.sun.jersey.api.client.WebResource;

public class TransactionResourceTest extends MetadataServerTestCase {

	public static class HelloWorld {
		@method("POST")
		public String hello(@type("*/*") String input) {
			return input + " world!";
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addBehaviour(HelloWorld.class,
				new URIImpl("urn:test:HelloWorld"));
		config.addBehaviour(NamedGraphSupport.class);
		super.setUp();
	}

	public void testPOST() throws Exception {
		WebResource path = client.path("interface");
		Model model = new LinkedHashModel();
		URI root = vf.createURI(path.getURI().toASCIIString());
		URI obj = vf.createURI("urn:test:HelloWorld");
		model.add(root, RDF.TYPE, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		assertEquals("hello world!", path.post(String.class, "hello"));
	}
}
