package org.callimachusproject.client;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import junit.framework.TestCase;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Test;

public class HttpOriginClientTest extends TestCase {
	private static final String ORIGIN = "http://example.com";
	private static final BasicStatusLine _200 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 200, "OK");
	private static final BasicStatusLine _302 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 302, "Found");
	private static final BasicStatusLine _401 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 401, "Unauthorized");
	private final Queue<HttpResponse> responses = new LinkedList<HttpResponse>();
	private final HttpOriginClient client = new HttpOriginClient(ORIGIN);
	private final FluidBuilder builder = FluidFactory.getInstance().builder();
	private final RequestDirector director = new RequestDirector() {
		public HttpResponse execute(HttpHost host, HttpRequest request,
				HttpContext ctx) throws HttpException, IOException {
			HttpResponse response = responses.poll();
			byte[] http = asByteArray(request);
			ByteArrayEntity entity = new ByteArrayEntity(http, ContentType.create("message/http"));
			response.setHeader(entity.getContentType());
			long length = entity.getContentLength();
			if (length >= 0) {
				response.setHeader("Content-Length", Long.toString(length));
			}
			response.setEntity(entity);
			return response;
		}
	};

	@Before
	public void setUp() throws Exception {
		responses.clear();
		HttpClientManager.getInstance().resetCache();
		HttpClientManager.getInstance().setProxy(
				new HttpHost("example.com", -1, "http"), director);
	}

	@After
	public void tearDown() throws Exception {
		HttpClientManager.getInstance().removeProxy(
				new HttpHost("example.com", -1, "http"), director);
	}

	@Test
	public void test200() throws Exception {
		responses.add(new BasicHttpResponse(_200));
		client.execute(new HttpGet("http://example.com/200"),
				new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				assertEquals(_200.getStatusCode(), response
						.getStatusLine().getStatusCode());
				return null;
			}
		});
	}

	@Test
	public void testTargetHost() throws Exception {
		HttpContext localContext = new BasicHttpContext();
		responses.add(new BasicHttpResponse(_200));
		client.execute(new HttpGet("http://example.com/200"),
				new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				assertEquals(_200.getStatusCode(), response
						.getStatusLine().getStatusCode());
				return null;
			}
		}, localContext);
		HttpHost host = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		assertEquals(ORIGIN, host.toString());
	}

	@Test
	public void testTarget() throws Exception {
		HttpContext localContext = new BasicHttpContext();
		responses.add(new BasicHttpResponse(_200));
		client.execute(new HttpGet("http://example.com/200"),
				new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				assertEquals(_200.getStatusCode(), response
						.getStatusLine().getStatusCode());
				return null;
			}
		}, localContext);
		HttpHost host = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		HttpUriRequest req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
		URI root = new URI(host.getSchemeName(), null, host.getHostName(), host.getPort(), "/", null, null);
		assertEquals("http://example.com/200", root.resolve(req.getURI()).toASCIIString());
	}

	@Test
	public void test302() throws Exception {
		responses.add(new BasicHttpResponse(_302));
		client.execute(new HttpGet("http://example.com/302"),
				new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				assertEquals(_302.getStatusCode(), response
						.getStatusLine().getStatusCode());
				return null;
			}
		});
	}

	@Test
	public void test302Redirect() throws Exception {
		HttpGet get = new HttpGet("http://example.com/302");
		get.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
		BasicHttpResponse redirect = new BasicHttpResponse(_302);
		redirect.setHeader("Location", "http://example.com/200");
		responses.add(redirect);
		responses.add(new BasicHttpResponse(_200));
		client.execute(get,
				new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				assertEquals(_200.getStatusCode(), response
						.getStatusLine().getStatusCode());
				return null;
			}
		});
	}

	@Test
	public void test302RedirectTarget() throws Exception {
		HttpContext localContext = new BasicHttpContext();
		HttpGet get = new HttpGet("http://example.com/302");
		get.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
		BasicHttpResponse redirect = new BasicHttpResponse(_302);
		redirect.setHeader("Location", "http://example.com/200");
		responses.add(redirect);
		responses.add(new BasicHttpResponse(_200));
		client.execute(get,
				new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				assertEquals(_200.getStatusCode(), response
						.getStatusLine().getStatusCode());
				return null;
			}
		}, localContext);
		HttpHost host = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		HttpUriRequest req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
		URI root = new URI(host.getSchemeName(), null, host.getHostName(), host.getPort(), "/", null, null);
		assertEquals("http://example.com/200", root.resolve(req.getURI()).toASCIIString());
	}

	@Test
	public void test302CachedRedirectTarget() throws Exception {
		do {
			HttpGet get = new HttpGet("http://example.com/302");
			get.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
			BasicHttpResponse redirect = new BasicHttpResponse(_302);
			redirect.setHeader("Location", "http://example.com/200");
			redirect.setHeader("Cache-Control", "public,max-age=3600");
			responses.add(redirect);
			BasicHttpResponse doc = new BasicHttpResponse(_200);
			doc.setHeader("Cache-Control", "public,max-age=3600");
			responses.add(doc);
			client.execute(get,
					new ResponseHandler<Void>() {
				public Void handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					assertEquals(_200.getStatusCode(), response
							.getStatusLine().getStatusCode());
					return null;
				}
			});
		} while (false);
		do {
			HttpContext localContext = new BasicHttpContext();
			HttpGet get = new HttpGet("http://example.com/302");
			get.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
			BasicHttpResponse redirect = new BasicHttpResponse(_302);
			redirect.setHeader("Location", "http://example.com/200");
			redirect.setHeader("Cache-Control", "public,max-age=3600");
			responses.add(redirect);
			client.execute(get,
					new ResponseHandler<Void>() {
				public Void handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					assertEquals(_302.getStatusCode(), response
							.getStatusLine().getStatusCode());
					return null;
				}
			}, localContext);
			HttpHost host = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			HttpUriRequest req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
			URI root = new URI(host.getSchemeName(), null, host.getHostName(), host.getPort(), "/", null, null);
			assertEquals("http://example.com/302", root.resolve(req.getURI()).toASCIIString());
		} while (false);
	}

	@Test
	public void testCookieStored() throws Exception {
		CookieStore cookieStore = new BasicCookieStore();
		do {
			HttpGet get = new HttpGet("http://example.com/setcookie");
			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
			get.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
			BasicHttpResponse setcookie = new BasicHttpResponse(_200);
			setcookie.addHeader("Set-Cookie", "oat=meal");
			setcookie.addHeader("Cache-Control", "no-store");
			responses.add(setcookie);
			client.execute(get,
					new ResponseHandler<Void>() {
				public Void handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					assertEquals(_200.getStatusCode(), response
							.getStatusLine().getStatusCode());
					assertTrue(response.containsHeader("Set-Cookie"));
					return null;
				}
			}, localContext);
		} while (false);
		do {
			HttpGet get = new HttpGet("http://example.com/getcookie");
			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
			get.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
			BasicHttpResponse getcookie = new BasicHttpResponse(_200);
			responses.add(getcookie);
			client.execute(get,
					new ResponseHandler<Void>() {
				public Void handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					assertEquals(_200.getStatusCode(), response
							.getStatusLine().getStatusCode());
					assertContains("oat=meal", asString(response.getEntity()));
					return null;
				}
			}, localContext);
		} while (false);
	}

	@Test
	public void testAuthentication() throws Exception {
		HttpGet get = new HttpGet("http://example.com/protected");
		HttpContext localContext = new BasicHttpContext();
		List<String> authpref = Collections.singletonList(AuthPolicy.BASIC);
		AuthScope scope = new AuthScope("example.com", -1);
		UsernamePasswordCredentials cred = new UsernamePasswordCredentials("Aladdin", "open sesame");
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(scope, cred);
		localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
		get.getParams().setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, true);
		get.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);
		BasicHttpResponse unauth = new BasicHttpResponse(_401);
		unauth.setHeader("WWW-Authenticate", "Basic realm=\"insert realm\"");
		responses.add(unauth);
		responses.add(new BasicHttpResponse(_200));
		client.execute(get,
				new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				assertEquals(_200.getStatusCode(), response
						.getStatusLine().getStatusCode());
				assertContains("Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==", asString(response.getEntity()));
				return null;
			}
		}, localContext);
	}

	byte[] asByteArray(HttpRequest request) throws IOException {
		try {
			String url = request.getRequestLine().getUri();
			if (url.startsWith("/")) {
				url = "http://" + request.getFirstHeader("Host").getValue() + url;
			}
			return (byte[]) builder.consume(request, url, HttpRequest.class, "message/http").as(byte[].class, "message/http");
		} catch (FluidException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	String asString(HttpEntity entity) throws IOException {
		try {
			String url = ORIGIN + "/entity";
			return builder.consume(entity, url, HttpEntity.class, "text/plain").asString("text/plain");
		} catch (FluidException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	void assertContains(String needle, String actual) {
		if (actual == null || !actual.contains(needle))
			throw new ComparisonFailure("", needle, actual);
	}

}
