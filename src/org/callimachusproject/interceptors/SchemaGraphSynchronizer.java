package org.callimachusproject.interceptors;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.behaviours.CalliObjectSupport;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.chain.HttpRequestChainInterceptor;
import org.openrdf.http.object.helpers.ObjectContext;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaGraphSynchronizer implements HttpRequestChainInterceptor {
	private final Logger logger = LoggerFactory
			.getLogger(SchemaGraphSynchronizer.class);

	@Override
	public HttpResponse intercept(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return null;
	}

	@Override
	public void process(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		String method = request.getRequestLine().getMethod();
		int statusCode = response.getStatusLine().getStatusCode();
		if ("GET".equals(method) || "HEAD".equals(method) || statusCode >= 400)
			return;
		ObjectContext ctx = ObjectContext.adapt(context);
		RDFObject target = ctx.getResourceTarget().getTargetObject();
		ObjectConnection con = target.getObjectConnection();
		try {
			Model schema = CalliObjectSupport.getSchemaModelFor(con);
			if (schema == null)
				return;
			synchronized (schema) {
				if (schema.isEmpty())
					return;
			}
			con.commit();
			con.begin();
			CalliRepository crepo = CalliObjectSupport
					.getCalliRepositroyFor(con.getRepository());
			synchronized (schema) {
				RepositoryConnection scon = crepo.openSchemaConnection();
				try {
					scon.begin();
					scon.clear(schema.contexts().toArray(new Resource[0]));
					for (Namespace ns : schema.getNamespaces()) {
						scon.setNamespace(ns.getPrefix(), ns.getName());
					}
					scon.add(schema);
					scon.commit();
					schema.clear();
				} finally {
					scon.close();
				}
			}
		} catch (OpenRDFException e) {
			logger.error(e.toString(), e);
		}
	}

}
