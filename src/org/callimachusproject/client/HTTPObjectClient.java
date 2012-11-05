package org.callimachusproject.client;

import static org.apache.http.client.params.ClientPNames.ALLOW_CIRCULAR_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.ClientPNames.DEFAULT_HEADERS;
import static org.apache.http.client.params.ClientPNames.HANDLE_AUTHENTICATION;
import static org.apache.http.client.params.ClientPNames.HANDLE_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.MAX_REDIRECTS;
import static org.apache.http.client.params.CookiePolicy.IGNORE_COOKIES;
import static org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.SOCKET_BUFFER_SIZE;
import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.STALE_CONNECTION_CHECK;
import static org.apache.http.params.CoreConnectionPNames.TCP_NODELAY;
import static org.apache.http.params.CoreProtocolPNames.USER_AGENT;
import static org.apache.http.params.CoreProtocolPNames.USE_EXPECT_CONTINUE;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.apache.http.impl.client.cache.FileResourceFactory;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.Version;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.server.exceptions.GatewayTimeout;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.server.util.FileUtil;
import org.callimachusproject.xml.ReusableDocumentResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the connections and cache for outgoing requests.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectClient extends AbstractHttpClient {
	private static final String DEFAULT_NAME = Version.getInstance().getVersion();
	private static int COUNT = 0;
	private static HTTPObjectClient instance;
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				synchronized (HTTPObjectClient.class) {
					if (instance != null) {
						instance.internal.getConnectionManager().shutdown();
						instance.storage.shutdown();
						instance = null;
					}
				}
			}
		}));
	}

	public static synchronized HTTPObjectClient getInstance()
			throws IOException {
		if (instance == null) {
			File dir = File.createTempFile("http-client-cache", "");
			dir.delete();
			FileUtil.deleteOnExit(dir);
			setCacheDirectory(dir);
		} else if (++COUNT % 100 == 0) {
			instance.cleanResources();
		}
		return instance;
	}

	public static synchronized void setCacheDirectory(File dir) {
		if (instance != null) {
			instance.internal.getConnectionManager().shutdown();
			instance.storage.shutdown();
		}
		dir.mkdirs();
		HttpParams params = getDefaultHttpParams();
		CacheConfig config = getDefaultCacheConfig();
		HttpClient client;
		client = new SystemDefaultHttpClient(params);
		client = new GZipHttpRequestClient(client);
		InternalHttpClient internal = new InternalHttpClient(client);
		FileResourceFactory entryFactory = new FileResourceFactory(dir);
		instance = new HTTPObjectClient(internal, config, entryFactory);
	}

	public static synchronized void invalidateCache() {
		resetCache();
	}

	public static synchronized void resetCache() {
		if (instance != null) {
			CacheConfig config = getDefaultCacheConfig();
			ResourceFactory entryFactory = instance.entryFactory;
			InternalHttpClient internal = instance.internal;
			instance = new HTTPObjectClient(internal, config, entryFactory);
		}
		ReusableDocumentResolver.invalidateCache();
	}

	private static HttpParams getDefaultHttpParams() {
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(SO_TIMEOUT, 0);
		params.setIntParameter(CONNECTION_TIMEOUT, 10000);
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024);
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false);
		params.setBooleanParameter(TCP_NODELAY, false);
		params.setBooleanParameter(USE_EXPECT_CONTINUE, true);
		params.setParameter(USER_AGENT, DEFAULT_NAME);
		params.setParameter(COOKIE_POLICY, IGNORE_COOKIES);
		params.setBooleanParameter(HANDLE_AUTHENTICATION, false);
		params.setBooleanParameter(HANDLE_REDIRECTS, false);
		params.setIntParameter(MAX_REDIRECTS, 20);
		params.setBooleanParameter(ALLOW_CIRCULAR_REDIRECTS, false);
		try {
			BasicHeader hd = new BasicHeader("From",
					System.getProperty("mail.from"));
			params.setParameter(DEFAULT_HEADERS, Collections.singleton(hd));
		} catch (SecurityException e) {
			// ignore
		}
		return params;
	}

	private static CacheConfig getDefaultCacheConfig() {
		int n = Runtime.getRuntime().availableProcessors();
		CacheConfig config = new CacheConfig();
		config.setSharedCache(true);
		config.setAsynchronousWorkersCore(1);
		config.setAsynchronousWorkersMax(n);
		config.setHeuristicCachingEnabled(true);
		config.setHeuristicDefaultLifetime(60 * 60 * 24);
		config.setMaxObjectSize(64000);
		return config;
	}

	private Logger logger = LoggerFactory.getLogger(HTTPObjectClient.class);
	private final HttpClient client;
	private final CachingHttpClient cache;
	private final InternalHttpClient internal;
	private final ResourceFactory entryFactory;
	private final ManagedHttpCacheStorage storage;

	private HTTPObjectClient(InternalHttpClient internal, CacheConfig config, ResourceFactory entryFactory) {
		this.internal = internal;
		this.entryFactory = entryFactory;
		storage = new ManagedHttpCacheStorage(config);
		cache = new CachingHttpClient(internal, entryFactory, storage, config);
		client = new GUnZipHttpResponseClient(cache);
	}

	@Override
	protected void finalize() throws Throwable {
		storage.shutdown();
	}

	/**
	 * Deletes the (no longer used) temporary cache files from disk.
	 */
	private void cleanResources() {
		storage.cleanResources();
	}

	public HttpClient setProxy(HttpHost destination, HttpClient proxy) {
		return internal.setProxy(destination, proxy);
	}

	public boolean removeProxy(HttpHost destination, HttpClient proxy) {
		return internal.removeProxy(destination, proxy);
	}

	/**
	 * Follows redirects and returns the final 200 or 203 response with the
	 * final request URL represented as the response header Content-Location
	 */
	public HttpResponse get(String url, String... accept)  throws IOException, ResponseException {
		String systemId = url;
		String redirect = systemId;
		HttpResponse resp = null;
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		for (int i = 0; i < 20 && redirect != null; i++) {
			systemId = redirect;
			HttpRequest req = new BasicHttpRequest("GET", url);
			if (accept != null && accept.length > 0) {
				for (String media : accept) {
					req.addHeader("Accept", media);
				}
			}
			resp = client.service(req);
			redirect = client.redirectLocation(redirect, resp);
		}
		int code = resp.getStatusLine().getStatusCode();
		if (code != 200 && code != 203)
			throw ResponseException.create(resp, systemId);
		resp.setHeader("Content-Location", systemId);
		return resp;
	}

	/**
	 * {@link HttpEntity#consumeContent()} or
	 * {@link HttpEntity#writeTo(java.io.OutputStream)} must be called if
	 * {@link HttpResponse#getEntity()} is non-null.
	 */
	public HttpResponse service(HttpRequest request) throws IOException,
			GatewayTimeout {
		try {
			return client.execute(determineTarget(request), request);
		} catch (java.net.ConnectException e) {
			throw new GatewayTimeout(e);
		}
	}

	public String redirectLocation(String base, HttpResponse resp) throws IOException,
			GatewayTimeout {
		int code = resp.getStatusLine().getStatusCode();
		if (code == 301 || code == 302 || code == 307 || code == 308) {
			Header location = resp.getFirstHeader("Location");
			if (location != null) {
				EntityUtils.consume(resp.getEntity());
				String value = location.getValue();
				if (value.startsWith("/") || !value.contains(":")) {
					try {
						value = TermFactory.newInstance(base).resolve(value);
					} catch (IllegalArgumentException e) {
						logger.warn(e.toString(), e);
						return value;
					}
				}
				return value;
			}
		}
		return null;
	}

	@Override
	public HttpResponse execute(HttpHost host, HttpRequest request,
			HttpContext context) throws IOException, ClientProtocolException {
		return client.execute(host, request, context);
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	@Override
	public HttpParams getParams() {
		return client.getParams();
	}

	private static HttpHost determineTarget(HttpRequest request)
			throws ClientProtocolException {
		// A null target may be acceptable if there is a default target.
		// Otherwise, the null target is detected in the director.
		HttpHost target = null;

		URI requestURI = URI.create(request.getRequestLine().getUri());
		if (requestURI.isAbsolute()) {
			target = URIUtils.extractHost(requestURI);
			if (target == null) {
				throw new ClientProtocolException(
						"URI does not specify a valid host name: " + requestURI);
			}
		}
		return target;
	}
}
