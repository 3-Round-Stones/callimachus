package org.callimachusproject.server.process;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.CloseableEntity;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.server.exceptions.ServiceUnavailable;
import org.callimachusproject.server.model.AsyncExecChain;
import org.callimachusproject.server.model.CalliContext;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.ResponseCallback;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryException;

public class RequestTransactionActor implements AsyncExecChain {
	private static final int ONE_PACKET = 1024;

	private final Map<String, CalliRepository> repositories = new LinkedHashMap<String, CalliRepository>();
	private final AsyncExecChain handler;
	final Executor executor;

	public RequestTransactionActor(AsyncExecChain handler, Executor executor) {
		this.handler = handler;
		this.executor = executor;
	}

	public synchronized void addOrigin(String origin, CalliRepository repository) {
		repositories.put(origin, repository);
	}

	public synchronized void removeOrigin(String origin) {
		repositories.remove(origin);
	}

	@Override
	public Future<CloseableHttpResponse> execute(HttpRoute route,
			HttpRequestWrapper request, HttpContext ctx,
			HttpExecutionAware execAware,
			FutureCallback<CloseableHttpResponse> callback) throws IOException,
			HttpException {
		final Request req = new Request(request);
		CalliRepository repo = getRepository(req.getOrigin());
		if (repo == null || !repo.isInitialized())
			throw new ServiceUnavailable("This origin is not configured");
		final CalliContext context = CalliContext.adapt(ctx);
		try {
			final ResourceOperation op = new ResourceOperation(req, repo);
			context.setResourceTransaction(op);
			op.begin(context.getReceivedOn(), req.getMethod(), req.isSafe());
			boolean success = false;
			try {
				Future<CloseableHttpResponse> future = handler.execute(route, request, context, execAware,
						new ResponseCallback(callback) {
							public void completed(CloseableHttpResponse result) {
								int code = result.getStatusLine()
										.getStatusCode();
								try {
									if (!req.isSafe() && code < 300) {
										op.commit();
									}
									createSafeHttpEntity(op, result);
									super.completed(result);
								} catch (RepositoryException ex) {
									failed(ex);
								} catch (IOException ex) {
									failed(ex);
								} finally {
									context.removeResourceTransaction(op);
								}
							}

							public void failed(Exception ex) {
								op.endExchange();
								context.removeResourceTransaction(op);
								super.failed(ex);
							}

							public void cancelled() {
								op.endExchange();
								context.removeResourceTransaction(op);
								super.cancelled();
							}
						});
				success = true;
				return future;
			} finally {
				if (!success) {
					op.endExchange();
				}
			}
		} catch (OpenRDFException ex) {
			throw new InternalServerError(ex);
		} catch (DatatypeConfigurationException ex) {
			throw new InternalServerError(ex);
		}
	}

	private synchronized CalliRepository getRepository(String origin) {
		return repositories.get(origin);
	}

	void createSafeHttpEntity(ResourceOperation req, HttpResponse resp) throws IOException {
		boolean endNow = true;
		try {
			if (resp.getEntity() != null) {
				int code = resp.getStatusLine().getStatusCode();
				HttpEntity entity = resp.getEntity();
				long length = entity.getContentLength();
				if (code < 300 && (length < 0 || length > ONE_PACKET)) {
					// chunk stream entity, close store connection later
					resp.setEntity(endEntity(entity, req));
					endNow = false;
				} else {
					// copy entity, close store now
					resp.setEntity(copyEntity(entity, (int) length));
					endNow = true;
				}
			} else {
				// no entity, close store now
				resp.setHeader("Content-Length", "0");
				endNow = true;
			}
		} finally {
			if (endNow) {
				req.endExchange();
			}
		}
	}

	private ByteArrayEntity copyEntity(HttpEntity entity, int length) throws IOException {
		InputStream in = entity.getContent();
		try {
			if (length < 0) {
				length = ONE_PACKET;
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
			ChannelUtil.transfer(in, baos);
			ByteArrayEntity bae = new ByteArrayEntity(baos.toByteArray());
			bae.setContentEncoding(entity.getContentEncoding());
			bae.setContentType(entity.getContentType());
			return bae;
		} finally {
			in.close();
		}
	}

	private CloseableEntity endEntity(HttpEntity entity,
			final ResourceOperation req) {
		return new CloseableEntity(entity, new Closeable() {
			public void close() {
				try {
					executor.execute(new Runnable() {
						public void run() {
							req.endExchange();
						}
					});
				} catch (RejectedExecutionException ex) {
					req.endExchange();
				}
			}
		});
	}

}
