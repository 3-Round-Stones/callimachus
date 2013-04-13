package org.callimachusproject.server;

import java.util.concurrent.atomic.AtomicLong;

import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.openrdf.annotations.Iri;

import com.sun.jersey.api.client.WebResource;

public class ResponseCacheTest extends MetadataServerTestCase {
	private WebResource display;
	private WebResource clock;
	private WebResource seq;

	@Iri("urn:mimetype:application/clock")
	public static class Clock {
		@Iri("urn:test:display")
		private Display display;

		@query("display")
		@requires("urn:test:grant")
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
		@requires("urn:test:grant")
		@header("Cache-Control:max-age=3")
		@Iri("urn:test:date")
		String getDate();

		void setDate(String date);

		@query("time")
		@requires("urn:test:grant")
		@Iri("urn:test:time")
		@header("Cache-Control:no-cache")
		String getTime();

		void setTime(String time);
	}

	@Iri("urn:mimetype:application/seq")
	public static class Seq {
		private static AtomicLong seq = new AtomicLong();

		@query("next")
		@requires("urn:test:grant")
		@type("text/plain")
		@header("Cache-Control:max-age=1")
		public String next() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@query("auth")
		@requires("urn:test:grant")
		@type("text/plain")
		@header("Cache-Control:max-age=10")
		public String auth() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@query("private")
		@requires("urn:test:grant")
		@type("text/plain")
		@header("Cache-Control:private")
		public String _private() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@query("number")
		@requires("urn:test:grant")
		@type("text/plain")
		@header("Cache-Control:public,max-age=1")
		public String number() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@query("seq")
		@requires("urn:test:grant")
		@type("text/plain")
		@header("Cache-Control:no-cache")
		public String seq() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@query("add")
		@requires("urn:test:grant")
		@type("text/plain")
		@header("Cache-Control:max-age=1")
		public String add(@header("amount") int amount) {
			return Long.toHexString(seq.addAndGet(amount));
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Seq.class);
		config.addConcept(Clock.class);
		config.addConcept(Display.class);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
		seq = client.path("/seq");
		seq.type("application/seq").put("cocky");
		display = client.path("/display");
		display.type("application/display").put("display");
		clock = client.path("/clock");
		clock.type("application/clock").put("clock");
		clock.queryParam("display", "").header("Content-Location",
				display.getURI()).type("text/uri-list").put(display.getURI().toASCIIString());
	}

	protected void addContentEncoding(WebResource client) {
		// seems to be a bug in this filter wrt Content-Length
		// client.addFilter(new GZIPContentEncodingFilter());
	}

	public void testNoCache() throws Exception {
		clock.queryParam("time", "").put("earlier");
		WebResource time = display.queryParam("time", "");
		String now = time.get(String.class);
		clock.queryParam("time", "").put("later");
		assertFalse(now.equals(time.get(String.class)));
	}

	public void testNoAuthorization() throws Exception {
		WebResource next = seq.queryParam("next", "");
		String first = next.get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testAuthorization() throws Exception {
		WebResource next = seq.queryParam("auth", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.header("Authorization", "second")
				.get(String.class);
		assertEquals(first, second);
	}

	public void testFirstAuthorization() throws Exception {
		WebResource next = seq.queryParam("auth", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testSecondAuthorization() throws Exception {
		WebResource next = seq.queryParam("auth", "");
		String first = next.get(String.class);
		String second = next.header("Authorization", "second")
				.get(String.class);
		assertEquals(first, second);
	}

	public void testPrivateAuthorization() throws Exception {
		WebResource next = seq.queryParam("private", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.header("Authorization", "second")
				.get(String.class);
		assertFalse(first.equals(second));
	}

	public void testFirstPrivateAuthorization() throws Exception {
		WebResource next = seq.queryParam("private", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.get(String.class);
		assertFalse(first.equals(second));
	}

	public void testSecondPrivateAuthorization() throws Exception {
		WebResource next = seq.queryParam("private", "");
		String first = next.get(String.class);
		String second = next.header("Authorization", "second")
				.get(String.class);
		assertFalse(first.equals(second));
	}

	public void testPublicNoAuthorization() throws Exception {
		WebResource next = seq.queryParam("number", "");
		String first = next.get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testPublicAuthorization() throws Exception {
		WebResource next = seq.queryParam("number", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.header("Authorization", "second")
				.get(String.class);
		assertEquals(first, second);
	}

	public void testPublicFirstAuthorization() throws Exception {
		WebResource next = seq.queryParam("number", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testPublicSecondAuthorization() throws Exception {
		WebResource next = seq.queryParam("number", "");
		String first = next.get(String.class);
		String second = next.header("Authorization", "second")
				.get(String.class);
		assertEquals(first, second);
	}

	public void testSameHeaderCache() throws Exception {
		WebResource next = seq.queryParam("add", "");
		String first = next.header("amount", 2).get(String.class);
		String second = next.header("amount", 2).get(String.class);
		assertEquals(first, second);
	}

	public void testDifferentHeaderCache() throws Exception {
		WebResource next = seq.queryParam("add", "");
		String first = next.header("amount", 2).get(String.class);
		String second = next.header("amount", 3).get(String.class);
		assertFalse(first.equals(second));
	}
}
