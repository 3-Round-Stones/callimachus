/**
 * 
 */
package org.openrdf.http.object;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.SSLServerIOEventDispatch;
import org.apache.http.impl.nio.codecs.HttpRequestParser;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HTTPSServerIOEventDispatch extends SSLServerIOEventDispatch {
	private Logger logger = LoggerFactory.getLogger(HTTPSServerIOEventDispatch.class);
	private static final String SCHEME = "http.protocol.scheme";
	private final HttpParams params;

	public HTTPSServerIOEventDispatch(NHttpServiceHandler handler,
			SSLContext sslcontext, HttpParams params) {
		super(handler, sslcontext, params);
		this.params = params;
	}

    @Override
    public void inputReady(IOSession session) {
        try {
            super.inputReady(session);
        } catch (RuntimeException ex) {
            session.shutdown();
            logger.error(ex.toString(), ex);
        }
    }

	@Override
	protected NHttpServerIOTarget createConnection(IOSession session) {
		return new DefaultNHttpServerConnection(session,
				createHttpRequestFactory(), createByteBufferAllocator(),
				this.params) {
			@Override
			protected NHttpMessageParser createRequestParser(
					SessionInputBuffer buffer,
					HttpRequestFactory requestFactory, HttpParams params) {
				return new HttpRequestParser(buffer, null,
						requestFactory, params) {
					@Override
					public HttpMessage parse() throws IOException,
							HttpException {
						return initializeRequest(super.parse(), session);
					}
				};
			}

			@Override
			public String toString() {
				return super.toString() + session.toString();
			}
		};
	}

	@Override
	protected HttpRequestFactory createHttpRequestFactory() {
		return new HttpRequestFactory() {
			public HttpRequest newHttpRequest(RequestLine requestline)
					throws MethodNotSupportedException {
				return new BasicHttpEntityEnclosingRequest(requestline);
			}

			public HttpRequest newHttpRequest(String method, String uri)
					throws MethodNotSupportedException {
				return new BasicHttpEntityEnclosingRequest(method, uri);
			};
		};
	}

	private HttpMessage initializeRequest(HttpMessage msg, IOSession session) {
		if (msg != null) {
			HttpParams params = msg.getParams();
			params.setParameter(SCHEME, "https");
			msg.setParams(params);
		}
		if (msg instanceof HttpEntityEnclosingRequest
				&& !msg.containsHeader("Content-Length")
				&& !msg.containsHeader("Transfer-Encoding")) {
			HttpEntityEnclosingRequest body = (HttpEntityEnclosingRequest) msg;
			BasicHttpRequest req = new BasicHttpRequest(body.getRequestLine());
			req.setHeaders(body.getAllHeaders());
			req.setParams(msg.getParams());
			return req;
		}
		return msg;
	}
}