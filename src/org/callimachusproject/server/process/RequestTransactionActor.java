package org.callimachusproject.server.process;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.server.CallimachusRepository;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.server.model.EntityRemovedHttpResponse;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.Response;
import org.callimachusproject.server.util.ManagedExecutors;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;

public class RequestTransactionActor extends ExchangeActor {
	private static final ProtocolVersion HTTP11 = HttpVersion.HTTP_1_1;
	private final Filter filter;
	private final Handler handler;
	private final CallimachusRepository repository;

	public RequestTransactionActor(Filter filter, Handler handler, CallimachusRepository repository) {
		this(new PriorityBlockingQueue<Runnable>(), filter, handler, repository);
	}

	private RequestTransactionActor(BlockingQueue<Runnable> queue, Filter filter, Handler handler, CallimachusRepository repository) {
		super(ManagedExecutors.newAntiDeadlockThreadPool(queue, "HttpTransaction"), queue);
		this.filter = filter;
		this.handler = handler;
		this.repository = repository;
	}

	protected void process(Exchange exchange, boolean foreground) throws Exception {
		Request req = exchange.getRequest();
		boolean success = false;
		final ResourceOperation op = new ResourceOperation(req, repository);
		try {
			op.begin();
			Response resp = handler.verify(op);
			if (resp == null) {
				exchange.verified();
				resp = handler.handle(op);
				if (resp.getStatusCode() >= 400) {
					op.rollback();
				} else if (!op.isSafe()) {
					op.commit();
				}
			}
			HttpResponse response = createSafeHttpResponse(op, resp);
			exchange.submitResponse(filter(req, response));
			success = true;
		} finally {
			if (!success) {
				op.endExchange();
			}
		}
	}

	private HttpResponse createSafeHttpResponse(final ResourceOperation req,
			Response resp) throws Exception {
		resp.onClose(new Runnable() {
			public void run() {
				req.endExchange();
			}
		});
		boolean content = false;
		HttpResponse response;
		try {
			response = createHttpResponse(req, resp);
			content = resp.isContent() && !resp.isException();
		} catch (ResponseException e) {
			response = createHttpResponse(req, new Response().exception(e));
		} catch (ConcurrencyException e) {
			response = createHttpResponse(req, new Response().conflict(e));
		} catch (IllegalArgumentException e) {
			// FIXME is this to general?
			response = createHttpResponse(req,
					new Response().status(406, "Not Acceptable"));
		} catch (Exception e) {
			response = createHttpResponse(req, new Response().server(e));
		} finally {
			if (!content) {
				resp.asVoid();
			}
		}
		return response;
	}

	private HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		HttpResponse resp = filter.filter(request, response);
		HttpEntity entity = resp.getEntity();
		if ("HEAD".equals(request.getMethod()) && entity != null) {
			EntityUtils.consume(entity);
			resp.setEntity(null);
		}
		return resp;
	}

	private HttpResponse createHttpResponse(Request req, Response resp)
			throws Exception {
		ProtocolVersion ver = HTTP11;
		int code = resp.getStatus();
		String phrase = resp.getMessage();
		HttpResponse response = new EntityRemovedHttpResponse(ver, code, phrase);
		for (Header hd : resp.getAllHeaders()) {
			response.addHeader(hd);
		}
		if (resp.isException()) {
			return createErrorResponse(req, resp.getException());
		} else if (resp.isContent()) {
            HttpEntity entity = resp.asHttpEntity();
            Header hd = entity.getContentType();
            if (hd != null) {
                    response.setHeader("Content-Type", hd.getValue());
            }
            long size = entity.getContentLength();
            if (size >= 0) {
                    response.setHeader("Content-Length", String.valueOf(size));
            } else if (!response.containsHeader("Content-Length")) {
                    response.setHeader("Transfer-Encoding", "chunked");
            }
            response.setEntity(entity);
		} else {
			response.setHeader("Content-Length", "0");
		}
		return response;
	}

}
