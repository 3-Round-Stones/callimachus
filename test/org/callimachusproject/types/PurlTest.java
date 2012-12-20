package org.callimachusproject.types;

import org.callimachusproject.test.TemporaryServerTestCase;
import org.callimachusproject.test.WebResource;

public class PurlTest extends TemporaryServerTestCase {
	private static int count = 0;
	private WebResource file;

	@Override
	public void setUp() throws Exception {
		file = null;
		super.setUp();
		String slug = "file" + ++count + ".txt";
		file = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection()
				.create(slug, "text/plain",
						"User-agent: *\nDisallow:".getBytes());
	}

	@Override
	public void tearDown() throws Exception {
		if (file != null) {
			file.delete();
		}
		super.tearDown();
	}

	public void testStaticCopy() throws Exception {
		WebResource purl = getHomeFolder().createPURL("humans.txt", "copy", file.toString());
		try {
			assertEquals(new String(file.get("text/plain")), new String(purl.get("text/plain")));
		} finally {
			purl.delete();
		}
	}

	public void testStaticCanonical() throws Exception {
		WebResource purl = getHomeFolder().createPURL("google", "canonical", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectLocation());
			assertEquals(301, purl.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testStaticAlternate() throws Exception {
		WebResource purl = getHomeFolder().createPURL("alternate", "alternate", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectLocation());
			assertEquals(302, purl.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testStaticDescribedBy() throws Exception {
		WebResource purl = getHomeFolder().createPURL("describing", "describedby", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectLocation());
			assertEquals(303, purl.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testStaticResides() throws Exception {
		WebResource purl = getHomeFolder().createPURL("resides", "resides", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectLocation());
			assertEquals(307, purl.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testStaticMoved() throws Exception {
		WebResource purl = getHomeFolder().createPURL("moved", "moved", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectLocation());
			assertEquals(308, purl.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testStaticMissing() throws Exception {
		WebResource purl = getHomeFolder().createPURL("missing", "missing", file.toString());
		try {
			assertEquals(404, purl.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testStaticGone() throws Exception {
		WebResource purl = getHomeFolder().createPURL("gone", "gone", file.toString());
		try {
			assertEquals(410, purl.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testPreserveQueryString() throws Exception {
		WebResource purl = getHomeFolder().createPURL("preserve", "alternate", "http://www.google.ca/search");
		try {
			assertEquals("http://www.google.ca/search?q=callimachus", purl.ref("?q=callimachus").getRedirectLocation());
			WebResource url = purl.ref("?search=callimachus");
			assertEquals(302, url.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testChangeQueryString() throws Exception {
		WebResource purl = getHomeFolder().createPURL("change", "alternate", "http://www.google.ca/search?q={search}");
		try {
			WebResource url = purl.ref("?search=callimachus");
			assertEquals("http://www.google.ca/search?q=callimachus", url.getRedirectLocation());
			assertEquals(302, url.headCode());
		} finally {
			purl.delete();
		}
	}

	public void testChainMissing() throws Exception {
		WebResource alt = getHomeFolder().createPURL("alt", "alternate", file.toString());
		WebResource missing = getHomeFolder().createPURL("chain", "missing", alt.toString());
		try {
			assertEquals(404, missing.headCode());
		} finally {
			missing.delete();
			alt.delete();
		}
	}

	public void testAcceptHeader() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n");
		sb.append("INSERT DATA {\n");
		sb.append("<accept>");
		sb.append(" a skos:Concept, </callimachus/Concept>;\n");
		sb.append("skos:prefLabel 'accept'\n");
		sb.append("}");
		WebResource concept = getHomeFolder().link("describedby").create("application/sparql-update", sb.toString().getBytes("UTF-8"));
		WebResource rdf = getHomeFolder().createPURL("accept.rdf", "copy", concept.toString() + "?describe\nAccept: application/rdf+xml");
		WebResource ttl = getHomeFolder().createPURL("accept.ttl", "copy", concept.toString() + "?describe\nAccept: text/turtle");
		try {
			String rdfxml = new String(concept.ref("?describe").get("application/rdf+xml"));
			String turtle = new String(concept.ref("?describe").get("text/turtle"));
			assertTrue(rdfxml.startsWith("<"));
			assertFalse(turtle.startsWith("<"));
			assertEquals(rdfxml, new String(rdf.get("*/*")));
			assertEquals(turtle, new String(ttl.get("*/*")));
		} finally {
			ttl.delete();
			rdf.delete();
			concept.delete();
		}
	}

}
