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
package org.callimachusproject.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.engine.model.TermFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Client protocol handling tests.
 */
public class HttpClientRedirectTest extends TestCase {
	private static final String ORIGIN = "http://example.com";
	private static final BasicStatusLine _200 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 200, "OK");
	private final HttpClient httpclient = HttpClientFactory.getInstance().createHttpClient(ORIGIN);
	private final Map<String, HttpRequestHandler> handlers = new HashMap<String, HttpRequestHandler>();
	private final ClientExecChain director = new ClientExecChain() {
		public CloseableHttpResponse execute(HttpRoute route,
				HttpRequestWrapper request, HttpClientContext context,
				HttpExecutionAware execAware) throws IOException, HttpException {
			HttpResponse response = new BasicHttpResponse(_200);
			String uri = request.getRequestLine().getUri();
			String path = URI.create(uri).getRawPath();
			HttpRequestHandler handler = handlers.get(path);
			if (handler == null) {
				handler = handlers.get("*");
			}
			if (handler != null) {
				handler.handle(request, response, context);
			}
			String systemId = uri;
			return new HttpUriResponse(systemId, response);
		}
	};

    private void register(String path, HttpRequestHandler handler) {
		handlers.put(path, handler);
	}

	@Before
	public void setUp() throws Exception {
		handlers.clear();
		HttpClientFactory.getInstance().setProxy(
				getServerHttp(), director);
	}

	@After
	public void tearDown() throws Exception {
		HttpClientFactory.getInstance().removeProxy(
				getServerHttp(), director);
	}

    /**
     * Obtains the address of the local test server.
     *
     * @return  the test server host, with a scheme name of "http"
     */
    protected HttpHost getServerHttp() {
        return new HttpHost("example.com", -1, "http");
    }

    private static class SimpleService implements HttpRequestHandler {

        public SimpleService() {
            super();
        }

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            response.setStatusCode(HttpStatus.SC_OK);
            final StringEntity entity = new StringEntity("Whatever");
            response.setEntity(entity);
        }
    }

    @Test
    public void testRedirectLocationsReset() throws Exception {
        register("*", new SimpleService());
        register("/People.htm", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", "/people.html");
                final StringEntity entity = new StringEntity("Whatever");
                response.setEntity(entity);
            }
        });
        final HttpHost target = getServerHttp();

        final HttpGet httpget = new HttpGet("/People.htm");
        final HttpClientContext context = HttpClientContext.create();

        this.httpclient.execute(target, httpget, context);
        // this checks that the context was properly reset
        final HttpResponse response = this.httpclient.execute(target, httpget, context);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        final HttpRequest request = context.getRequest();
        Assert.assertEquals("/people.html", request.getRequestLine().getUri());

        final URI uri = new URIBuilder()
            .setHost(target.getHostName())
            .setPort(target.getPort())
            .setScheme(target.getSchemeName())
            .setPath("/people.html")
            .build();

        final URI location = getHttpLocation(httpget, context);
        Assert.assertEquals(uri, location);
    }

	@Test
    public void testHttpFragmentReset() throws Exception {
        register("*", new SimpleService());
        register("/~tim", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_SEE_OTHER);
                response.setHeader("Location", "/People.htm#tim");
                final StringEntity entity = new StringEntity("Whatever");
                response.setEntity(entity);
            }
        });
        register("/People.htm", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", "/people.html");
                final StringEntity entity = new StringEntity("Whatever");
                response.setEntity(entity);
            }
        });
        final HttpHost target = getServerHttp();

        final HttpClientContext context = HttpClientContext.create();

        this.httpclient.execute(target, new HttpGet("/~tim"), context);
        // this checks that the context was properly reset
        final HttpResponse response = this.httpclient.execute(target, new HttpGet("/People.htm"), context);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        final HttpRequest request = context.getRequest();
        Assert.assertEquals("/people.html", request.getRequestLine().getUri());

        final URI uri = new URIBuilder()
            .setHost(target.getHostName())
            .setPort(target.getPort())
            .setScheme(target.getSchemeName())
            .setPath("/people.html")
            .build();

        final URI location = getHttpLocation(new HttpGet("/People.htm"), context);
        Assert.assertEquals(uri, location);
    }

    @Test
    public void testRelativeRequestURIWithFragment() throws Exception {
        register("*", new SimpleService());
        final HttpHost target = getServerHttp();

        final HttpGet httpget = new HttpGet("/stuff#blahblah");
        final HttpClientContext context = HttpClientContext.create();

        final HttpResponse response = this.httpclient.execute(target, httpget, context);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        final HttpRequest request = context.getRequest();
        Assert.assertEquals("/stuff", request.getRequestLine().getUri());

        final URI uri = new URIBuilder(httpget.getURI())
            .setHost(target.getHostName())
            .setPort(target.getPort())
            .setScheme(target.getSchemeName())
            .build();

        final URI location = getHttpLocation(httpget, context);
        Assert.assertEquals(uri, location);
    }

    @Test
    public void testAbsoluteRequestURIWithFragment() throws Exception {
        register("*", new SimpleService());
        final HttpHost target = getServerHttp();

        final URI uri = new URIBuilder()
            .setHost(target.getHostName())
            .setPort(target.getPort())
            .setScheme(target.getSchemeName())
            .setPath("/stuff")
            .setFragment("blahblah")
            .build();

        final HttpGet httpget = new HttpGet(uri);
        final HttpClientContext context = HttpClientContext.create();

        final HttpResponse response = this.httpclient.execute(httpget, context);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        final HttpRequest request = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
        Assert.assertEquals("/stuff", request.getRequestLine().getUri());

        final URI location = getHttpLocation(httpget, context);
        Assert.assertEquals(uri, location);
    }

    @Test
    public void testRedirectFromFragment() throws Exception {
        register("*", new SimpleService());
        register("/People.htm", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", "/people.html");
                final StringEntity entity = new StringEntity("Whatever");
                response.setEntity(entity);
            }
        });
        final HttpHost target = getServerHttp();

        final HttpGet httpget = new HttpGet("/People.htm#tim");
        final HttpClientContext context = HttpClientContext.create();

        final HttpResponse response = this.httpclient.execute(target, httpget, context);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        final HttpRequest request = context.getRequest();
        Assert.assertEquals("/people.html", request.getRequestLine().getUri());

        final URI uri = new URIBuilder()
            .setHost(target.getHostName())
            .setPort(target.getPort())
            .setScheme(target.getSchemeName())
            .setPath("/people.html")
            .setFragment("tim")
            .build();

        final URI location = getHttpLocation(httpget, context);
        Assert.assertEquals(uri, location);
    }

    @Test
    public void testRedirectWithFragment() throws Exception {
        register("*", new SimpleService());
        register("/~tim", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_SEE_OTHER);
                response.setHeader("Location", "/People.htm#tim");
                final StringEntity entity = new StringEntity("Whatever");
                response.setEntity(entity);
            }
        });
        register("/People.htm", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", "/people.html");
                final StringEntity entity = new StringEntity("Whatever");
                response.setEntity(entity);
            }
        });
        final HttpHost target = getServerHttp();

        final HttpGet httpget = new HttpGet("/~tim");
        final HttpClientContext context = HttpClientContext.create();

        final HttpResponse response = this.httpclient.execute(target, httpget, context);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        final HttpRequest request = context.getRequest();
        Assert.assertEquals("/people.html", request.getRequestLine().getUri());

        final URI uri = new URIBuilder()
            .setHost(target.getHostName())
            .setPort(target.getPort())
            .setScheme(target.getSchemeName())
            .setPath("/people.html")
            .setFragment("tim")
            .build();

        final URI location = getHttpLocation(httpget, context);
        Assert.assertEquals(uri, location);
    }

    /**
     * The interpreted (absolute) URI that was used to generate the last
     * request.
     */
	private URI getHttpLocation(HttpUriRequest originalRequest,
			HttpClientContext ctx) {
		try {
			URI original = originalRequest.getURI();
			HttpHost target = ctx.getTargetHost();
			List<URI> redirects = ctx.getRedirectLocations();
			URI absolute = URIUtils.resolve(original, target, redirects);
			return new URI(TermFactory.newInstance(absolute.toASCIIString()).getSystemId());
		} catch (URISyntaxException e) {
			return null;
		}
	}

}
