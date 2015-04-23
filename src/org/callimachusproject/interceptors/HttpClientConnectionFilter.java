package org.callimachusproject.interceptors;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.client.HttpClientDependent;
import org.openrdf.http.client.SesameClientImpl;
import org.openrdf.http.object.chain.HttpRequestChainInterceptor;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.http.object.helpers.ObjectContext;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.helpers.SailConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientConnectionFilter implements HttpRequestChainInterceptor {
	private final Logger logger = LoggerFactory.getLogger(HttpClientConnectionFilter.class);
	private final ExecutorService executor = Executors.newCachedThreadPool();

	@Override
	public HttpResponse intercept(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		ObjectContext ctx = ObjectContext.adapt(context);
		CalliObject target = (CalliObject) ctx.getResourceTarget().getTargetObject();
		try {
			HttpUriClient client = target.getHttpClient();
			ObjectConnection con = target.getObjectConnection();
			FederatedServiceResolverImpl resolver = new FederatedServiceResolverImpl();
			resolver.setSesameClient(new SesameClientImpl(client, executor));
			setClient(con, resolver);
		} catch (OpenRDFException e) {
			logger.error(e.toString(), e);
		}
		return null;
	}

	@Override
	public void process(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		// HttpClient is setup to auto-close
	}

	private void setClient(RepositoryConnection con,
			FederatedServiceResolverImpl resolver) {
		if (con instanceof SailRepositoryConnection) {
			setClient(((SailRepositoryConnection) con).getSailConnection(),
					resolver);
		} else if (con instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) con)
					.setFederatedServiceResolver(resolver);
		} else if (con instanceof HttpClientDependent) {
			((HttpClientDependent) con).setHttpClient(resolver.getHttpClient());
		} else if (con instanceof RepositoryConnectionWrapper) {
			setClient(((RepositoryConnectionWrapper) con).getDelegate(),
					resolver);
		}
	}

	private void setClient(SailConnection con,
			FederatedServiceResolverImpl resolver) {
		if (con instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) con)
					.setFederatedServiceResolver(resolver);
		} else if (con instanceof HttpClientDependent) {
			((HttpClientDependent) con).setHttpClient(resolver.getHttpClient());
		} else if (con instanceof SailConnectionWrapper) {
			setClient(((SailConnectionWrapper) con).getWrappedConnection(),
					resolver);
		}
	}

}
