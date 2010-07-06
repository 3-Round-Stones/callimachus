package org.callimachusproject.behaviours;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.concepts.Template;
import org.callimachusproject.traits.Realm;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.BehaviourException;

public abstract class RealmSupport implements Realm, RDFObject {

	private static final ProtocolVersion HTTP11 = new ProtocolVersion("HTTP",
			1, 1);
	private static final BasicStatusLine _401 = new BasicStatusLine(HTTP11,
			401, "Unauthorized");
	private static final BasicStatusLine _403 = new BasicStatusLine(HTTP11,
			403, "Forbidden");

	@Override
	public String protectionDomain() {
		StringBuilder sb = new StringBuilder();
		for (Object domain : getCalliDomains()) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(domain.toString());
		}
		if (sb.length() < 1)
			return null;
		return sb.toString();
	}

	@Override
	public String allowOrigin() {
		StringBuilder sb = new StringBuilder();
		for (Object origin : getCalliOrigins()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(origin.toString());
		}
		if (sb.length() < 1)
			return null;
		return sb.toString();
	}

	@Override
	// TODO remove in alibaba 2.0-beta3
	public HttpResponse unauthorized() throws IOException {
		try {
			return unauthorized(this);
		} catch (Exception exc) {
			throw new BehaviourException(exc);
		}
	}

	@Override
	// TODO remove in alibaba 2.0-beta3
	public HttpResponse forbidden() throws IOException {
		try {
			return forbidden(this);
		} catch (Exception exc) {
			throw new BehaviourException(exc);
		}
	}

	public HttpResponse unauthorized(Object target) throws Exception {
		Template unauthorized = getCalliUnauthorized();
		if (unauthorized == null)
			return null;
		Reader reader = unauthorized.calliConstruct("unauthorized", target);
		try {
			CharArrayWriter writer = new CharArrayWriter();
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
			}
			HttpResponse resp = new BasicHttpResponse(_401);
			resp.setHeader("Cache-Control", "no-store");
			resp.setHeader("Content-Type", "text/html;charset=\"UTF-8\"");
			resp.setEntity(new StringEntity(writer.toString(), "UTF-8"));
			return resp;
		} finally {
			reader.close();
		}
	}

	public HttpResponse forbidden(Object target) throws Exception {
		Template forbidden = getCalliForbidden();
		if (forbidden == null)
			return null;
		Reader reader = forbidden.calliConstruct("forbidden", target);
		try {
			CharArrayWriter writer = new CharArrayWriter();
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
			}
			HttpResponse resp = new BasicHttpResponse(_403);
			resp.setHeader("Cache-Control", "no-store");
			resp.setHeader("Content-Type", "text/html;charset=\"UTF-8\"");
			resp.setEntity(new StringEntity(writer.toString(), "UTF-8"));
			return resp;
		} finally {
			reader.close();
		}
	}

	@Override
	public Object authenticateAgent(String method, String via,
			Set<String> names, String algorithm, byte[] encoded)
			throws RepositoryException {
		return null;
	}

}
