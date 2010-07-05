package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.callimachusproject.traits.OriginRealm;
import org.openrdf.repository.RepositoryException;

public abstract class OriginRealmSupport implements OriginRealm {

	@Override
	public String protectionDomain() {
		StringBuilder sb = new StringBuilder();
		for (Object domain : getDomains()) {
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
		for (Object origin : getOrigins()) {
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
	public HttpResponse unauthorized() throws IOException {
		return null;
	}

	@Override
	public HttpResponse forbidden() throws IOException {
		return null;
	}

	@Override
	public Object authenticateAgent(String method, String via, Set<String> names,
			String algorithm, byte[] encoded) throws RepositoryException {
		return null;
	}

}
