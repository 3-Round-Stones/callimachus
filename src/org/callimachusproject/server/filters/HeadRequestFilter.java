package org.callimachusproject.server.filters;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;

public class HeadRequestFilter implements HttpResponseInterceptor {

	@Override
	public void process(HttpResponse response, HttpContext context)
			throws HttpException, IOException {
		HttpRequest request = HttpCoreContext.adapt(context).getRequest();
		HttpEntity entity = response.getEntity();
		if ("HEAD".equals(request.getRequestLine().getMethod())
				&& entity != null) {
			EntityUtils.consumeQuietly(entity);
			response.setEntity(null);
		}
	}

}
