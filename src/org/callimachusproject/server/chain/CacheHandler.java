package org.callimachusproject.server.chain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpAsyncClient;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.conn.ClientAsyncConnectionManager;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.AutoClosingAsyncClient;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.callimachusproject.server.util.HTTPDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheHandler implements AsyncExecChain {
	private final class DelegatingClient extends
			CloseableHttpAsyncClient {
		private final AsyncExecChain delegate;
		private boolean running;
		public DelegatingClient(AsyncExecChain delegate) {
			this.delegate = delegate;
		}

		public void start() {
			running = true;
		}

		public void close() {
			shutdown();
		}

		public void shutdown() {
			running = false;
		}

		public boolean isRunning() {
			return running;
		}

		public IOReactorStatus getStatus() {
			return null;
		}

		public HttpParams getParams() {
			return null;
		}

		public ClientAsyncConnectionManager getConnectionManager() {
			return null;
		}

		public <T> Future<T> execute(HttpAsyncRequestProducer arg0,
				HttpAsyncResponseConsumer<T> arg1, HttpContext arg2,
				FutureCallback<T> arg3) {
			throw new UnsupportedOperationException("CachingHttpAsyncClient does not caching for streaming HTTP exchanges");
		}

		public Future<HttpResponse> execute(HttpHost target,
				final HttpRequest request, final HttpContext context,
				FutureCallback<HttpResponse> callback) {
			return delegate.execute(target, request, context, callback);
		}
	}

	private final Logger logger = LoggerFactory.getLogger(CacheHandler.class);
	private final HTTPDateFormat modifiedformat = new HTTPDateFormat();
	private final AsyncExecChain delegate;
	private final ResourceFactory resourceFactory;
	private final CacheConfig config;
	private final Map<HttpHost, HttpAsyncClient> clients = new HashMap<HttpHost, HttpAsyncClient>();

	public CacheHandler(AsyncExecChain delegate, ResourceFactory resourceFactory, CacheConfig config) {
		this.delegate = delegate;
		this.resourceFactory = resourceFactory;
		this.config = config;
	}

	public synchronized void reset() {
		clients.clear();
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target,
			final HttpRequest request, final HttpContext context,
			FutureCallback<HttpResponse> callback) {
		if (config.isHeuristicCachingEnabled()) {
			return getClient(target).execute(target, request, context,
					new ResponseCallback(callback) {
						public void completed(HttpResponse result) {
							setCacheControlIfCacheable(request, result, context);
							super.completed(result);
						}
					});
		} else {
			return getClient(target)
					.execute(target, request, context, callback);
		}
	}

	private synchronized HttpAsyncClient getClient(HttpHost target) {
		if (clients.containsKey(target))
			return clients.get(target);
		logger.debug("Initializing server side cache for {}", target);
		ManagedHttpCacheStorage storage = new ManagedHttpCacheStorage(config);
		CachingHttpAsyncClient cachingClient = new CachingHttpAsyncClient(new DelegatingClient(delegate), resourceFactory, storage, config);
		HttpAsyncClient client = new AutoClosingAsyncClient(cachingClient, storage);
		clients.put(target, client);
		return client;
	}

	/**
	 * Adds max-age cache-control based on last-modified heuristic for client
	 * caching.
	 */
	void setCacheControlIfCacheable(final HttpRequest request,
			HttpResponse response, final HttpContext context) {
		String method = request.getRequestLine().getMethod();
		int sc = response.getStatusLine().getStatusCode();
		if ("GET".equals(method)) {
			switch (sc) {
			case 200:
			case 203:
			case 206:
			case 300:
			case 301:
			case 302:
			case 303:
			case 307:
			case 308:
			case 410:
				setCacheControl(response, context);
			case 304:
				if (response.getFirstHeader("Cache-Control") != null) {
					setCacheControl(response, context);
				}
			}
		}
	}

	private void setCacheControl(HttpResponse response, final HttpContext context) {
		long now = CalliContext.adapt(context).getReceivedOn();
		Header lastMod = response.getLastHeader("Last-Modified");
		Header[] headers = response.getHeaders("Cache-Control");
		if (headers != null && headers.length > 0 && now > 0 && lastMod != null) {
			String cc = getCacheControl(headers, lastMod, now);
			if (cc != null) {
				response.removeHeaders("Cache-Control");
				response.setHeader("Cache-Control", cc);
			}
		}
	}

	private String getCacheControl(Header[] cache, Header lastMod, long now) {
		StringBuilder sb = new StringBuilder();
		for (Header hd : cache) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(hd.getValue());
		}
		if (sb.length() == 0 || sb.indexOf("max-age") < 0
				&& sb.indexOf("s-maxage") < 0 && sb.indexOf("no-cache") < 0
				&& sb.indexOf("no-store") < 0) {
			int maxage = getMaxAgeHeuristic(lastMod, now);
			if (maxage > 0) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append("max-age=").append(maxage);
				return sb.toString();
			}
		}
		return null;
	}

	private int getMaxAgeHeuristic(Header lastModified, long now) {
		long lm = lastModified(lastModified);
		int fraction = (int) ((now - lm) / 10000);
		return Math.min(fraction, 24 * 60 * 60);
	}

	private long lastModified(Header lastModified) {
		return modifiedformat.parseHeader(lastModified);
	}

}
