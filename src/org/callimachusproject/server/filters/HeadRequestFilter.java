package org.callimachusproject.server.filters;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;

public class HeadRequestFilter extends Filter {

	public HeadRequestFilter(Filter delegate) {
		super(delegate);
	}

	@Override
	public HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		HttpResponse resp = super.filter(request, response);
		HttpEntity entity = resp.getEntity();
		if ("HEAD".equals(request.getMethod()) && entity != null) {
			EntityUtils.consume(entity);
			resp.setEntity(null);
		}
		return resp;
	}

}
