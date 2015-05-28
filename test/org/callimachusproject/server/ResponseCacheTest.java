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

import java.util.concurrent.atomic.AtomicLong;

import org.callimachusproject.annotations.requires;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.openrdf.annotations.Header;
import org.openrdf.annotations.HeaderParam;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Method;
import org.openrdf.annotations.Path;
import org.openrdf.annotations.Type;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;

import com.sun.jersey.api.client.WebResource;

public class ResponseCacheTest extends MetadataServerTestCase {
	private WebResource display;
	private WebResource clock;
	private WebResource seq;

	@Iri("urn:mimetype:application/clock")
	public static abstract class Clock implements RDFObject {
		@Iri("urn:test:display")
		private Display display;

		@Method("GET")
		@Path("?display")
		@requires("urn:test:grant")
		@Type("text/uri-list")
		public String getDisplay() {
			return display.toString();
		}

		@Method("PUT")
		@Path("?display")
		@requires("urn:test:grant")
		public void setDisplay(@Type("text/uri-list") String display)
				throws RepositoryException, QueryEvaluationException {
			this.display = getObjectConnection().getObject(Display.class,
					display);
		}

		@Method("PUT")
		@Path("?date")
		@requires("urn:test:grant")
		public void setDate(@Type("*/*") String date) {
			display.setDate(date);
		}

		@Method("PUT")
		@Path("?time")
		@requires("urn:test:grant")
		public void setTime(@Type("*/*") String time) {
			display.setTime(time);
		}
	}

	@Iri("urn:mimetype:application/display")
	public interface Display {

		@Method("GET")
		@Path("?date")
		@Header("Cache-Control:max-age=3")
		@requires("urn:test:grant")
		@Iri("urn:test:date")
		String getDate();

		void setDate(String date);

		@Method("GET")
		@Path("?time")
		@Header("Cache-Control:no-cache")
		@requires("urn:test:grant")
		@Iri("urn:test:time")
		String getTime();

		void setTime(String time);
	}

	@Iri("urn:mimetype:application/seq")
	public static class Seq {
		private static AtomicLong seq = new AtomicLong();

		@Method("GET")
		@Path("?next")
		@Header("Cache-Control:max-age=1")
		@requires("urn:test:grant")
		@Type("text/plain")
		public String next() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@Method("GET")
		@Path("?auth")
		@Header("Cache-Control:max-age=10")
		@requires("urn:test:grant")
		@Type("text/plain")
		public String auth() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@Method("GET")
		@Path("?private")
		@Header("Cache-Control:private")
		@requires("urn:test:grant")
		@Type("text/plain")
		public String _private() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@Method("GET")
		@Path("?number")
		@Header("Cache-Control:public,max-age=1")
		@requires("urn:test:grant")
		@Type("text/plain")
		public String number() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@Method("GET")
		@Path("?seq")
		@Header("Cache-Control:no-cache")
		@requires("urn:test:grant")
		@Type("text/plain")
		public String seq() {
			return Long.toHexString(seq.incrementAndGet());
		}

		@Method("GET")
		@Path("?add")
		@Header("Cache-Control:max-age=1")
		@requires("urn:test:grant")
		@Type("text/plain")
		public String add(@HeaderParam("amount") int amount) {
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
