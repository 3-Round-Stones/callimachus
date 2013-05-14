package org.callimachusproject.client;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpCacheClient extends OverloadedHttpClient implements HttpClient {
	final Logger logger = LoggerFactory.getLogger(HttpCacheClient.class);
	private final HttpClient client;
	private final ManagedHttpCacheStorage storage;

	public HttpCacheClient(HttpClient client, ResourceFactory factory,
			CacheConfig config) {
		this.storage = new ManagedHttpCacheStorage(config);
		this.client = new CachingHttpClient(client, factory,
				this.storage, config);
	}

	@Override
	protected void finalize() throws Throwable {
		storage.shutdown();
	}

	public void cleanResources() {
		storage.cleanResources();
	}

	public void shutdown() {
		storage.shutdown();
	}

	@Override
	public HttpResponse execute(final HttpHost host, final HttpRequest request,
			HttpContext ctx) throws IOException, ClientProtocolException {
		if (ctx != null) {
			try {
				ctx.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
				ctx.setAttribute(ExecutionContext.HTTP_REQUEST, new RequestWrapper(request));
			} catch (ProtocolException e) {
				throw new ClientProtocolException(e);
			}
		}
		HttpResponse resp = client.execute(host, request, ctx);
		HttpEntity entity = resp.getEntity();
		if (entity != null) {
			resp.setEntity(new CloseableEntity(entity, new Closeable() {
				public void close() throws IOException {
					// this also keeps this object from being finalized
					// until all its response entities are consumed
					String uri = request.getRequestLine().getUri();
					logger.debug("Remote {}{} closed", host, uri);
				}
			}));
		}
		return resp;
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	@Override
	public HttpParams getParams() {
		return client.getParams();
	}

}
