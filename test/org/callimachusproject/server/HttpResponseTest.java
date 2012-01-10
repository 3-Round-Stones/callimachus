package org.callimachusproject.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.client.HTTPObjectClient;
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
			ProtocolVersion HTTP11 = new ProtocolVersion("HTTP", 1, 1);
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
		HTTPObjectClient.getInstance().setEnvelopeType("message/x-response");
		con = repository.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public void testEcho() throws Exception {
		Echo echo = (Echo) con.getObject(client.path("/echo").toString());
		HttpResponse resp = echo.echo("text/alpha", "abc");
		assertEquals("text/alpha", resp.getFirstHeader("Content-Type").getValue());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		HttpEntity entity = resp.getEntity();
		try {
			entity.writeTo(out);
		} finally {
			entity.consumeContent();
		}
		assertEquals("abc", out.toString("UTF-8"));
	}
}
