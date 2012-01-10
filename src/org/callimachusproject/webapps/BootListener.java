package org.callimachusproject.webapps;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootListener extends UploadListener {
	private static final String NS = "http://callimachusproject.org/rdf/2009/framework#";

	private Logger logger = LoggerFactory.getLogger(BootListener.class);
	private HTTPObjectClient client;
	private InetSocketAddress proxy;
	private String authorization;
	private boolean started;

	public BootListener() throws IOException {
		client = HTTPObjectClient.getInstance();
	}

	public void setProxy(InetSocketAddress proxy) {
		this.proxy = proxy;
	}

	public void setAuthorization(String authorization) {
		this.authorization = authorization;
	}

	@Override
	public void notifyStarted() {
		try {
			started = true;
			HttpRequest req = new BasicHttpRequest("POST", NS + "boot?started");
			req.setHeader("Authorization", authorization);
			HttpResponse resp = client.service(proxy, req);
			StatusLine status = resp.getStatusLine();
			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
			System.gc();
			if (status.getStatusCode() != 204) {
				logger.error(status.getReasonPhrase() + " once started");
			}
		} catch (Exception exc) {
			logger.error(exc.toString(), exc);
		}
	}

	@Override
	public void notifyReloaded() {
		try {
			if (!started)
				return;
			HttpRequest req = new BasicHttpRequest("POST", NS + "boot?reloaded");
			req.setHeader("Authorization", authorization);
			HttpResponse resp = client.service(proxy, req);
			StatusLine status = resp.getStatusLine();
			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
			System.gc();
			if (status.getStatusCode() != 204) {
				logger.error(status.getReasonPhrase() + " once reloaded");
			}
		} catch (Exception exc) {
			logger.error(exc.toString(), exc);
		}
	}

	@Override
	public void notifyStopping() {
		try {
			started = false;
			HttpRequest req = new BasicHttpRequest("POST", NS + "boot?stopping");
			req.setHeader("Authorization", authorization);
			HttpResponse resp = client.service(proxy, req);
			StatusLine status = resp.getStatusLine();
			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
			if (status.getStatusCode() != 204) {
				logger.error(status.getReasonPhrase() + " while stopping");
			}
		} catch (Exception exc) {
			logger.error(exc.toString(), exc);
		}
	}

}
