package org.callimachusproject.client;

import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

public class StoreFragementRedirectStrategy extends DefaultRedirectStrategy {
	public static final String HTTP_LOCATION = "http.redirect.location";

	@Override
	public URI getLocationURI(HttpRequest request, HttpResponse response,
			HttpContext context) throws ProtocolException {
		URI target = super.getLocationURI(request, response, context);
		if (target != null) {
	        //get the location header to find out where to redirect to
	        Header locationHeader = response.getFirstHeader("location");
	        String location = locationHeader.getValue();
	        URI uri = createLocationURI(location);
	        context.setAttribute(HTTP_LOCATION, uri);
		}
		return target;
	}

}
