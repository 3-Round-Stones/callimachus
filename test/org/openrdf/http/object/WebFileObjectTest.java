package org.openrdf.http.object;

import java.io.IOException;
import java.io.Writer;

import javax.tools.FileObject;

import org.openrdf.annotations.Iri;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

public class WebFileObjectTest extends MetadataServerTestCase {

	@Iri("urn:test:Item")
	public static abstract class Item implements FileObject {
		@method("PUT")
		public void setBody(@type("*/*") String body) throws IOException {
			Writer writer = openWriter();
			writer.write(body);
			writer.close();
		}
	}

	@Iri("urn:test:Container")
	public static abstract class Container implements RDFObject, FileObject {
		@method("POST")
		public void addItem(@type("text/plain") String body) throws RepositoryException, IOException {
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
