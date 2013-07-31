package org.callimachusproject.server.chain;

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
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.CloseableEntity;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.server.exceptions.ServiceUnavailable;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.Request;
import org.callimachusproject.server.helpers.RequestActivityFactory;
import org.callimachusproject.server.helpers.ResourceOperation;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionHandler implements AsyncExecChain {
	private static final int ONE_PACKET = 1024;

	private final Logger logger = LoggerFactory.getLogger(ResourceOperation.class);
	private final Map<String, CalliRepository> repositories = new LinkedHashMap<String, CalliRepository>();
	private final AsyncExecChain handler;
	final Executor executor;

	public TransactionHandler(AsyncExecChain handler, Executor executor) {
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
	public Future<HttpResponse> execute(HttpHost target,
			HttpRequest request, HttpContext ctx,
			FutureCallback<HttpResponse> callback) {
		final Request req = new Request(request);
		final boolean unsafe = !req.isSafe();
		CalliRepository repo = getRepository(req.getOrigin());
		if (repo == null || !repo.isInitialized())
			throw new ServiceUnavailable("This origin is not configured");
		final CalliContext context = CalliContext.adapt(ctx);
		try {
			context.setCalliRepository(repo);
			final ObjectConnection con = repo.getConnection();
			con.begin();
			long now = context.getReceivedOn();
			if (unsafe) {
				initiateActivity(now, con, context);
			}
			context.setObjectConnection(con);
			final ResourceOperation op = new ResourceOperation(req, con);
			context.setResourceTransaction(op);
			boolean success = false;
			try {
				Future<HttpResponse> future = handler.execute(target, request, context, new ResponseCallback(callback) {
					public void completed(HttpResponse result) {
						int code = result.getStatusLine()
								.getStatusCode();
						try {
							createSafeHttpEntity(result, unsafe && code < 400, con);
							super.completed(result);
						} catch (RepositoryException ex) {
							failed(ex);
						} catch (IOException ex) {
							failed(ex);
						} catch (RuntimeException ex) {
							failed(ex);
						} finally {
							context.setResourceTransaction(null);
							context.setObjectConnection(null);
							context.setCalliRepository(null);
						}
					}

					public void failed(Exception ex) {
						endTransaction(con);
						context.setResourceTransaction(null);
						context.setObjectConnection(null);
						context.setCalliRepository(null);
						super.failed(ex);
					}

					public void cancelled() {
						endTransaction(con);
						context.setResourceTransaction(null);
						context.setObjectConnection(null);
						context.setCalliRepository(null);
						super.cancelled();
					}
				});
				success = true;
				return future;
			} finally {
				if (!success) {
					endTransaction(con);
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

	void createSafeHttpEntity(HttpResponse resp, boolean commit,
			ObjectConnection con) throws IOException, RepositoryException {
		boolean endNow = true;
		try {
			if (resp.getEntity() != null) {
				int code = resp.getStatusLine().getStatusCode();
				HttpEntity entity = resp.getEntity();
				long length = entity.getContentLength();
				if ((code == 200 || code == 203)
						&& (length < 0 || length > ONE_PACKET)) {
					// chunk stream entity, close store connection later
					resp.setEntity(endEntity(entity, con));
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
			if (commit) {
				con.commit();
			}
			if (endNow) {
				endTransaction(con);
			}
		}
	}

	/**
	 * Request has been fully read and response has been fully written.
	 */
	public void endTransaction(ObjectConnection con) {
		try {
			if (con.isOpen()) {
				con.rollback();
				con.close();
			}
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
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
			final ObjectConnection con) {
		return new CloseableEntity(entity, new Closeable() {
			public void close() {
				try {
					executor.execute(new Runnable() {
						public void run() {
							endTransaction(con);
						}
					});
				} catch (RejectedExecutionException ex) {
					endTransaction(con);
				}
			}
		});
	}

	private void initiateActivity(long now, ObjectConnection con, CalliContext ctx) throws RepositoryException,
			DatatypeConfigurationException {
		AuditingRepositoryConnection audit = findAuditing(con);
		if (audit != null) {
			ActivityFactory delegate = audit.getActivityFactory();
			URI bundle = con.getVersionBundle();
			assert bundle != null;
			URI activity = delegate.createActivityURI(bundle, con.getValueFactory());
			con.setVersionBundle(bundle); // use the same URI for blob version
			audit.setActivityFactory(new RequestActivityFactory(activity, delegate, ctx, now));
		}
	}

	private AuditingRepositoryConnection findAuditing(
			RepositoryConnection con) throws RepositoryException {
		if (con instanceof AuditingRepositoryConnection)
			return (AuditingRepositoryConnection) con;
		if (con instanceof RepositoryConnectionWrapper)
			return findAuditing(((RepositoryConnectionWrapper) con).getDelegate());
		return null;
	}

}
