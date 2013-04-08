package org.callimachusproject.client;

import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

public class HttpUriEntity extends HttpEntityWrapper {
	private final String systemId;

	public HttpUriEntity(String systemId, HttpEntity wrapped) {
		super(wrapped);
		this.systemId = systemId;
	}

	public String getSystemId() {
		return systemId;
	}

	public URI getURI() {
		return URI.create(systemId);
	}
}
