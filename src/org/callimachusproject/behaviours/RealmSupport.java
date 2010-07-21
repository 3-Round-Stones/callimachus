package org.callimachusproject.behaviours;

import java.io.CharArrayWriter;
import java.io.Reader;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.concepts.Template;
import org.callimachusproject.traits.Realm;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

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
		for (Object origin : getCalliScripts()) {
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
	public boolean withAgentCredentials(String origin) {
		for (Object script : getCalliOrigins()) {
			String ao = script.toString();
			if (origin.startsWith(ao) || ao.startsWith(origin)
					&& ao.charAt(origin.length()) == '/')
				return true;
		}
		return false;
	}

	@Override
	public Object authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException {
		String[] vias = request.get("via");
		if (vias == null || vias.length < 1)
			return null;
		String via = vias[vias.length - 1];
		assert via != null;
		ObjectFactory of = getObjectConnection().getObjectFactory();
		int idx = via.lastIndexOf(' ');
		if (idx > 0 && idx < via.length() - 1)
			return of.createObject("dns:" + via.substring(idx + 1));
		return null;
	
	}

	@Override
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
			StringEntity entity = new StringEntity(writer.toString(), "UTF-8");
			entity.setContentType("text/html;charset=\"UTF-8\"");
			HttpResponse resp = new BasicHttpResponse(_401);
			resp.setHeader("Cache-Control", "no-store");
			resp.setEntity(entity);
			return resp;
		} finally {
			reader.close();
		}
	}

	@Override
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
			StringEntity entity = new StringEntity(writer.toString(), "UTF-8");
			entity.setContentType("text/html;charset=\"UTF-8\"");
			HttpResponse resp = new BasicHttpResponse(_403);
			resp.setHeader("Cache-Control", "no-store");
			resp.setEntity(entity);
			return resp;
		} finally {
			reader.close();
		}
	}

}
