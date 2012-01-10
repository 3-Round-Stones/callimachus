package org.callimachusproject.server;

import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.openrdf.annotations.Iri;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class ContentMD5Test extends MetadataServerTestCase {

	@Iri(RDFS.NAMESPACE + "Resource")
	public interface Resource {
		@query("property")
		@type("text/plain")
		@Iri("urn:test:property")
		String getProperty();

		@query("property")
		@Iri("urn:test:property")
		void setProperty(@type("text/plain") String property);
	}

	public void setUp() throws Exception {
		config.addConcept(Resource.class);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
	}

	protected void addContentEncoding(WebResource client) {
		// no encoding
	}

	public void testGoodMD5() throws Exception {
		WebResource web = client.path("/");
		web.header("Content-MD5", "nwqq6b6ua/tTDk7B5M184w==").put(
				"Check Integrity!");
	}

	public void testBadMD5() throws Exception {
		try {
			WebResource web = client.path("/");
			web.header("Content-MD5", "WRH4uNnoR4EfM9mheXUtIA==").put(
					"Check Integrity!");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(400, e.getResponse().getStatus());
		}
	}

	public void testGoodMD5Property() throws Exception {
		WebResource web = client.path("/").queryParam("property", "");
		web.header("Content-MD5", "nwqq6b6ua/tTDk7B5M184w==").put(
				"Check Integrity!");
	}

	public void testBadMD5Property() throws Exception {
		try {
			WebResource web = client.path("/").queryParam("property", "");
			web.header("Content-MD5", "WRH4uNnoR4EfM9mheXUtIA==").put(
					"Check Integrity!");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(400, e.getResponse().getStatus());
		}
	}
}
