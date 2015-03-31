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

import java.io.IOException;
import java.io.Writer;

import javax.tools.FileObject;

import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Method;
import org.openrdf.annotations.Type;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

public class WebFileObjectTest extends MetadataServerTestCase {

	@Iri("urn:test:Item")
	public static abstract class Item implements FileObject {
		@Method("PUT")
		public void setBody(@Type("*/*") String body) throws IOException {
			Writer writer = openWriter();
			writer.write(body);
			writer.close();
		}
	}

	@Iri("urn:test:Container")
	public static abstract class Container implements RDFObject, FileObject {
		@Method("POST")
		public void addItem(@Type("text/plain") String body) throws RepositoryException, IOException {
			ObjectConnection con = getObjectConnection();
			Object obj = con.getObject(toUri().resolve("item").toASCIIString());
			Item item = con.addDesignation(obj, Item.class);
			con.commit();
			Writer writer = item.openWriter();
			writer.write(body);
			writer.close();
		}
	}

	private ObjectConnection con;

	public void setUp() throws Exception {
		config.addConcept(Container.class);
		config.addConcept(Item.class);
		super.setUp();
		con = repository.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public void testRemoteFileUpload() throws Exception {
		String uri = client.path("/container").toString();
		Container container = con.addDesignation(con.getObject(uri),
				Container.class);
		container.addItem("body");
	}
}
