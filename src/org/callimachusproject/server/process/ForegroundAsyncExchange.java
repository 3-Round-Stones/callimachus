package org.callimachusproject.server.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;

public class ForegroundAsyncExchange implements HttpAsyncExchange {
	private final List<Cancellable> cancellables = new ArrayList<Cancellable>();
	private final HttpRequest request;
	private HttpResponse response;
	private boolean completed;
	private int timeout;

	public ForegroundAsyncExchange(HttpRequest request) {
		this.request = request;
	}

	@Override
	public HttpRequest getRequest() {
		return request;
	}

	@Override
	public HttpResponse getResponse() {
		return response;
	}

	@Override
	public void submitResponse() {
		completed = true;
	}

	@Override
	public void submitResponse(HttpAsyncResponseProducer responseProducer) {
		response = responseProducer.generateResponse();
	}

	@Override
	public boolean isCompleted() {
		return completed;
	}

	@Override
	public void setCallback(Cancellable cancellable) {
		cancellables.add(cancellable);
	}

	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

}
