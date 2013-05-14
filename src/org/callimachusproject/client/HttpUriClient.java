package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.callimachusproject.server.exceptions.ResponseException;

public interface HttpUriClient extends HttpClient {

	HttpUriEntity getEntity(String url, String accept) throws IOException,
			ResponseException;

	HttpUriResponse getResponse(HttpUriRequest request) throws IOException,
			ResponseException;
}
