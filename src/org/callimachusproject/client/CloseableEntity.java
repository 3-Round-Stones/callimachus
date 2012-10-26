package org.callimachusproject.client;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.HttpEntity;

public class CloseableEntity extends HttpEntityWrapper {
	private final Closeable closeable;

	public CloseableEntity(HttpEntity entity, Closeable closeable) {
		super(entity);
		this.closeable = closeable;
	}

	@Override
	protected void finish() throws IOException {
		closeable.close();
	}

}
