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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Matching;
import org.openrdf.repository.object.ObjectConnection;

public class HttpResponseTest extends MetadataServerTestCase {

	private ObjectConnection con;

	@Matching("/echo")
	public static class Echo {
		@method("POST")
		@type("message/x-response")
		public HttpResponse echo(@header("Content-Type") String type,
				@type("*/*") String body) throws IOException {
			ProtocolVersion HTTP11 = HttpVersion.HTTP_1_1;
			BasicStatusLine line = new BasicStatusLine(HTTP11, 200, "OK");
			HttpResponse resp = new BasicHttpResponse(line);;
			NStringEntity entity = new NStringEntity(body, "UTF-8");
			entity.setContentType(type);
			resp.setEntity(entity);
			return resp;
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Echo.class);
		super.setUp();
		server.setEnvelopeType("message/x-response");
		con = repository.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public void testEcho() throws Exception {
		Echo echo = (Echo) con.getObject(client.path("/echo").toString());
		HttpResponse resp = echo.echo("text/alpha", "abc");
		assertEquals("text/alpha", resp.getEntity().getContentType().getValue());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		HttpEntity entity = resp.getEntity();
		try {
			entity.writeTo(out);
		} finally {
			EntityUtils.consume(entity);
		}
		assertEquals("abc", out.toString("UTF-8"));
	}
}
