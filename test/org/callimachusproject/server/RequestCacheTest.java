package org.callimachusproject.server;

import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.rel;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Sparql;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class RequestCacheTest extends MetadataServerTestCase {

	private WebResource display;
	private WebResource clock;

	@Iri("urn:mimetype:application/clock")
	public static class Clock {
		@Iri("urn:test:display")
		private Display display;

		@query("display")
		@requires("urn:test:grant")
		@type("text/uri-list")
		public Display getDisplay() {
			return display;
		}

		@query("display")
		@requires("urn:test:grant")
		public void setDisplay(@type("*/*") Display display) {
			this.display = display;
		}

		@query("date")
		@requires("urn:test:grant")
		public void setDate(@type("*/*") String date) {
			display.setDate(date);
		}

		@query("time")
		@requires("urn:test:grant")
		public void setTime(@type("*/*") String time) {
			display.setTime(time);
		}
	}

	@Iri("urn:mimetype:application/display")
	public interface Display {
		@query("date")
		@header("Cache-Control:max-age=3")
		@Iri("urn:test:date")
		@requires("urn:test:grant")
		@type("text/plain")
		String getDate();

		void setDate(String date);

		@query("time")
		@Iri("urn:test:time")
		@requires("urn:test:grant")
		@type("text/plain")
		String getTime();

		void setTime(String time);

		@rel("alternate")
		@query("construct")
		@requires("urn:test:grant")
		@type("application/rdf+xml")
		@Sparql("DESCRIBE $this")
		GraphQueryResult construct();

		@rel("alternate")
		@query("select")
		@requires("urn:test:grant")
		@type("application/sparql-results+xml")
		@Sparql("SELECT ?date ?time WHERE { $this <urn:test:date> ?date ; <urn:test:time> ?time }")
		TupleQueryResult select();
	}

	public void setUp() throws Exception {
		config.addConcept(Clock.class);
		config.addConcept(Display.class);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
		display = client.path("/display");
		display.type("application/display").put("display");
		clock = client.path("/clock");
		clock.type("application/clock").put("clock");
		clock.queryParam("display", "").type("text/uri-list").put(display.getURI().toASCIIString());
	}

	protected void addContentEncoding(WebResource client) {
		// seems to be a bug in this filter wrt Content-Length
		// client.addFilter(new GZIPContentEncodingFilter());
	}

	public void testStale() throws Exception {
		clock.queryParam("time", "").type("text/plain").put("earlier");
		WebResource time = display.queryParam("time", "");
		String now = time.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("time", "").type("text/plain").put("later");
		assertFalse(now.equals(time.get(String.class)));
	}

	public void testResponseMaxAge() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource date = display.queryParam("date", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("date", "").put("later");
		assertEquals(now, date.get(String.class));
	}

	public void testRequestMaxAge() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource time = display.queryParam("date", "");
		String now = time.get(String.class);
		Thread.sleep(2000);
		clock.queryParam("date", "").put("later");
		assertFalse(now.equals(time.header("cache-control", "max-age=1").get(
				String.class)));
	}

	public void testRequestNoCache() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource date = display.queryParam("date", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("date", "").put("later");
		assertFalse(now.equals(date.header("cache-control", "no-cache").get(
				String.class)));
	}

	public void testMaxStale() throws Exception {
		clock.queryParam("time", "").put("earlier");
		WebResource date = display.queryParam("time", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("time", "").put("later");
		ClientResponse resp = date.header("cache-control", "max-stale").get(
				ClientResponse.class);
		assertEquals(now, resp.getEntity(String.class));
		assertTrue(resp.getHeaders().get("Warning").toString().contains("110"));
	}

	public void testMinFresh() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource date = display.queryParam("date", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("date", "").put("later");
		assertFalse(now.equals(date.header("cache-control", "min-fresh=3").get(
				String.class)));
	}

	public void testOnlyCached() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource date = display.queryParam("date", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("date", "").put("later");
		assertEquals(now, date.header("cache-control", "only-if-cached").get(
				String.class));
	}

	public void testOnlyNotCached() throws Exception {
		clock.queryParam("time", "").put("earlier");
		WebResource date = display.queryParam("time", "");
		Thread.sleep(1000);
		clock.queryParam("time", "").put("later");
		try {
			date.header("cache-control", "only-if-cached").get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(504, e.getResponse().getStatus());
		}
	}

	public void testSeeAlso() throws Exception {
		ClientResponse tuple = display.accept("application/sparql-results+xml").get(ClientResponse.class);
		assertEquals("application/sparql-results+xml", tuple.getType().toString());
		String tupleTag = tuple.getEntityTag().toString();
		ClientResponse graph = display.accept("application/rdf+xml").get(ClientResponse.class);
		assertEquals("application/rdf+xml", graph.getType().toString());
		String graphTag = graph.getEntityTag().toString();
		assertFalse(tupleTag.equals(graphTag));
	}

	public void testInvalidate() throws Exception {
		clock.queryParam("date", "").type("text/plain").put("earlier");
		WebResource date = display.queryParam("date", "");
		String earlier = date.get(String.class);
		clock.queryParam("date", "").type("text/plain").put("later");
		clock.queryParam("display", "").header("Content-Location",
				display.getURI()).type("text/uri-list").put(display.getURI().toASCIIString());
		String later = date.get(String.class);
		assertFalse(earlier.equals(later));
	}

}
