package org.callimachusproject.server.chain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

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
				HttpRequest request, HttpContext context,
				FutureCallback<HttpResponse> callback) {
			return delegate.execute(target, request, context, callback);
		}
	}

	private final Logger logger = LoggerFactory.getLogger(CacheHandler.class);
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
			HttpRequest request, HttpContext context,
			FutureCallback<HttpResponse> callback) {
		return getClient(target).execute(target, request, context, callback);
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

}
