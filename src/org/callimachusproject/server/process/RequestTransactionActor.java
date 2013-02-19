package org.callimachusproject.server.process;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.CloseableEntity;
import org.callimachusproject.concurrent.ManagedExecutors;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.exceptions.NotFound;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.server.model.EntityRemovedHttpResponse;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.Response;

public class RequestTransactionActor extends ExchangeActor {
	private static final ProtocolVersion HTTP11 = HttpVersion.HTTP_1_1;
	private final Map<String, CalliRepository> repositories = new LinkedHashMap<String, CalliRepository>();
	private final Filter filter;
	private final Handler handler;

	public RequestTransactionActor(Filter filter, Handler handler) {
		this(new PriorityBlockingQueue<Runnable>(), filter, handler);
	}

	private RequestTransactionActor(BlockingQueue<Runnable> queue,
			Filter filter, Handler handler) {
		super(ManagedExecutors.getInstance().newAntiDeadlockThreadPool(queue,
				"HttpTransaction"), queue);
		this.filter = filter;
		this.handler = handler;
	}

	public synchronized void addOrigin(String origin, CalliRepository repository) {
		repositories.put(origin, repository);
	}

	protected void process(Exchange exchange, boolean foreground)
			throws Exception {
		Request req = exchange.getRequest();
		boolean success = false;
		CalliRepository repo = getRepository(req.getOrigin());
		final ResourceOperation op = new ResourceOperation(req, repo);
		try {
			op.begin();
			Response resp = handler.verify(op);
			if (resp == null) {
				exchange.verified(op.getCredential());
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

	protected HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		HttpResponse resp = filter.filter(request, response);
		HttpEntity entity = resp.getEntity();
		if ("HEAD".equals(request.getMethod()) && entity != null) {
			EntityUtils.consume(entity);
			resp.setEntity(null);
		}
		return resp;
	}

	private synchronized CalliRepository getRepository(String origin) throws NotFound {
		if (repositories.containsKey(origin))
			return repositories.get(origin);
		if (repositories.isEmpty())
			throw new NotFound("Origins not configured");
		return repositories.values().iterator().next();
	}

	private HttpResponse createSafeHttpResponse(final ResourceOperation req,
			Response resp) throws Exception {
		boolean content = false;
		HttpResponse response;
		try {
			if (resp.isException())
				return createErrorResponse(req, resp.getException());
			ProtocolVersion ver = HTTP11;
			int code = resp.getStatus();
			String phrase = resp.getMessage();
			response = new EntityRemovedHttpResponse(ver, code, phrase);
			for (Header hd : resp.getAllHeaders()) {
				response.addHeader(hd);
			}
			if (resp.isContent()) {
				HttpEntity entity = resp.asHttpEntity();
				response.setEntity(new CloseableEntity(entity, new Closeable() {
					public void close() {
						req.endExchange();
					}
				}));
				content = true;
			} else {
				response.setHeader("Content-Length", "0");
			}
		} catch (Exception e) {
			return createErrorResponse(req, e);
		} finally {
			if (!content) {
				req.endExchange();
			}
		}
		return response;
	}

	@Override
	protected ResponseException asResponseException(Request req, Exception e) {
		// FIXME is this to general?
		if (e instanceof IllegalArgumentException)
			return new NotAcceptable(e);
		return super.asResponseException(req, e);
	}

}
