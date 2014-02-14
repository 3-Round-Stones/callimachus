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
package org.callimachusproject.server.helpers;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.cache.CachingHttpAsyncClient;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.CloseableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoClosingAsyncClient extends CloseableHttpAsyncClient {
	final Logger logger = LoggerFactory.getLogger(AutoClosingAsyncClient.class);
	private final CachingHttpAsyncClient client;
	private final ManagedHttpCacheStorage storage;
	private int numberOfClientCalls = 0;
	private boolean running;

	public AutoClosingAsyncClient(CachingHttpAsyncClient client, ManagedHttpCacheStorage storage) {
		this.client = client;
		this.storage = storage;
	}

	@Override
	protected void finalize() throws Throwable {
		shutdown();
	}

	/**
	 * Deletes the (no longer used) temporary cache files from disk.
	 */
	public void cleanResources() {
		storage.cleanResources();
	}

    public void shutdown() {
		running = false;
		logger.debug("Disposing of obsolete cache entries");
		storage.shutdown();
    }

	@Override
	public void close() throws IOException {
		shutdown();
	}

	@Override
	public void start() {
		running = true;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public <T> Future<T> execute(HttpAsyncRequestProducer requestProducer,
			HttpAsyncResponseConsumer<T> responseConsumer, HttpContext context,
			FutureCallback<T> callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target, HttpRequest request,
			HttpContext context, FutureCallback<HttpResponse> callback) {
		return client.execute(target, request, context, track(callback));
	}

	private FutureCallback<HttpResponse> track(FutureCallback<HttpResponse> callback) {
		return new ResponseCallback(callback) {
			public void completed(HttpResponse result) {
				try {
					HttpEntity entity = result.getEntity();
					if (entity != null) {
						result.setEntity(new CloseableEntity(entity, new Closeable() {
							public void close() throws IOException {
								// this also keeps this object from being finalized
								// until all its response entities are consumed
								if (++numberOfClientCalls % 100 == 0) {
									cleanResources();
								}
							}
						}));
					}
					super.completed(result);
				} catch (RuntimeException ex) {
					super.failed(ex);
				}
			}
		};
	}

}
