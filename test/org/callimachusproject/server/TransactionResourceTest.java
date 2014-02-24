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
package org.callimachusproject.server;

import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.NamedGraphSupport;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import com.sun.jersey.api.client.WebResource;

public class TransactionResourceTest extends MetadataServerTestCase {

	public static class HelloWorld {
		@method("POST")
		@requires("urn:test:grant")
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
