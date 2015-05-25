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
package org.callimachusproject.types;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;

public class PurlIntegrationTest extends TemporaryServerIntegrationTestCase {
	private static int count = 0;
	private WebResource file;

	@Override
	public void setUp() throws Exception {
		file = null;
		super.setUp();
		String slug = "file" + ++count + ".txt";
		file = getHomeFolder()
				.rel("contents", "application/atom+xml")
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
		WebResource purl = getHomeFolder().createPurl("humans.txt", "copy", file.toString());
		try {
			assertEquals(new String(file.get("text/plain")), new String(purl.get("text/plain")));
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testStaticCanonical() throws Exception {
		WebResource purl = getHomeFolder().createPurl("google", "canonical", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectTarget().toString());
			assertEquals(301, purl.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testStaticAlternate() throws Exception {
		WebResource purl = getHomeFolder().createPurl("alternate", "alternate", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectTarget().toString());
			assertEquals(302, purl.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testStaticDescribedBy() throws Exception {
		WebResource purl = getHomeFolder().createPurl("describing", "describedby", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectTarget().toString());
			assertEquals(303, purl.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testStaticResides() throws Exception {
		WebResource purl = getHomeFolder().createPurl("resides", "resides", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectTarget().toString());
			assertEquals(307, purl.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testStaticMoved() throws Exception {
		WebResource purl = getHomeFolder().createPurl("moved", "moved", "http://www.google.ca/");
		try {
			assertEquals("http://www.google.ca/", purl.getRedirectTarget().toString());
			assertEquals(308, purl.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testStaticMissing() throws Exception {
		WebResource purl = getHomeFolder().createPurl("missing", "missing", file.toString());
		try {
			assertEquals(404, purl.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testStaticGone() throws Exception {
		WebResource purl = getHomeFolder().createPurl("gone", "gone", file.toString());
		try {
			assertEquals(410, purl.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testPreserveQueryString() throws Exception {
		WebResource purl = getHomeFolder().createPurl("preserve", "alternate", "http://www.google.ca/search");
		try {
			assertEquals("http://www.google.ca/search?q=callimachus", purl.ref("?q=callimachus").getRedirectTarget().toString());
			WebResource url = purl.ref("?search=callimachus");
			assertEquals(302, url.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testChangeQueryString() throws Exception {
		WebResource purl = getHomeFolder().createPurl("change", "alternate", "http://www.google.ca/search?q={search}");
		try {
			WebResource url = purl.ref("?search=callimachus");
			assertEquals("http://www.google.ca/search?q=callimachus", url.getRedirectTarget().toString());
			assertEquals(302, url.headCode());
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testChainMissing() throws Exception {
		WebResource alt = getHomeFolder().createPurl("alt", "alternate", file.toString());
		WebResource missing = getHomeFolder().createPurl("chain", "missing", alt.toString());
		try {
			assertEquals(404, missing.headCode());
		} finally {
			missing.rel("describedby").delete();
			alt.rel("describedby").delete();
		}
	}

	public void testAcceptHeader() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli: <http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n");
		sb.append("<accept>");
		sb.append(" a skos:Concept, </callimachus/Concept>;\n");
		sb.append("skos:prefLabel 'accept'.\n");
		WebResource concept = getHomeFolder().rel("describedby").create("text/turtle", sb.toString().getBytes("UTF-8")).rev("describedby");
		WebResource rdf = getHomeFolder().createPurl("accept.rdf", "copy", concept.toString() + "?describe\nAccept: application/rdf+xml");
		WebResource ttl = getHomeFolder().createPurl("accept.ttl", "copy", concept.toString() + "?describe\nAccept: text/turtle");
		try {
			String rdfxml = new String(concept.ref("?describe").get("application/rdf+xml"));
			String turtle = new String(concept.ref("?describe").get("text/turtle"));
			assertTrue(rdfxml.startsWith("<"));
			assertFalse(turtle.startsWith("<"));
			assertEquals(rdfxml, new String(rdf.get("*/*")));
			assertEquals(turtle, new String(ttl.get("*/*")));
		} finally {
			ttl.rel("describedby").delete();
			rdf.rel("describedby").delete();
			concept.rel("describedby").delete();
		}
	}

	public void testPost() throws Exception {
		WebResource purl = getHomeFolder().createPurl("humans.txt", "put", "POST .* " + file.toString());
		try {
			byte[] bytes = "post via purl".getBytes();
			purl.post("text/plain", bytes);
			assertEquals(new String(bytes), new String(file.get("text/plain")));
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testPut() throws Exception {
		WebResource purl = getHomeFolder().createPurl("humans.txt", "put", file.toString());
		try {
			byte[] bytes = "put via purl".getBytes();
			purl.put("text/plain", bytes);
			assertEquals(new String(bytes), new String(file.get("text/plain")));
		} finally {
			purl.rel("describedby").delete();
		}
	}

	public void testDelete() throws Exception {
		WebResource purl = getHomeFolder().createPurl("humans.txt", "delete", file.toString());
		try {
			purl.delete();
			assertEquals(404, file.headCode());
			file = null;
		} finally {
			purl.rel("describedby").delete();
		}
	}

}
