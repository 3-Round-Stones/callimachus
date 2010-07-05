package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.openrdf.http.object.traits.Realm;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;

public abstract class NobodyRealmSupport implements Realm, RDFObject {

	@Override
	public String protectionDomain() {
		return null;
	}

	@Override
	public String allowOrigin() {
		return null;
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

	@Override
	public Object authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException {
		return null;
	}

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, String qs) {
		return false;
	}

}
