package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.openrdf.http.object.traits.Realm;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

public abstract class AnybodyRealmSupport implements Realm, RDFObject {

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
		assert via != null;
		ObjectFactory of = getObjectConnection().getObjectFactory();
		int idx = via.lastIndexOf(' ');
		if (idx > 0 && idx < via.length() - 1)
			return of.createObject("dns:" + via.substring(idx + 1));
		return null;
	}

	@Override
	public Object authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException {
		String[] vias = request.get("via");
		if (vias == null || vias.length < 1)
			return null;
		String via = vias[vias.length - 1];
		return authenticateAgent(method, via, null, null, null);
	}

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, String qs) {
		if (credential instanceof RDFObject) {
			RDFObject o = (RDFObject) credential;
			return o.getResource().stringValue().startsWith("dns:");
		}
		return false;
	}

}
