package org.callimachusproject.client;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoClosingHttpClient extends CloseableHttpClient {
	final Logger logger = LoggerFactory.getLogger(AutoClosingHttpClient.class);
	private final CloseableHttpClient client;
	private final ManagedHttpCacheStorage storage;
	private int numberOfClientCalls = 0;

	public AutoClosingHttpClient(CloseableHttpClient client, ManagedHttpCacheStorage storage) {
		this.client = client;
		this.storage = storage;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	public void cleanResources() {
		storage.cleanResources();
	}

    public void close() throws IOException {
		client.close();
		storage.shutdown();
    }

	@Override
	protected CloseableHttpResponse doExecute(final HttpHost host, final HttpRequest request,
            HttpContext ctx) throws IOException, ClientProtocolException {
		if (++numberOfClientCalls % 100 == 0) {
			// Deletes the (no longer used) temporary cache files from disk.
			cleanResources();
		}
		CloseableHttpResponse resp = client.execute(host, request, ctx);
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
