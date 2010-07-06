package org.callimachusproject.behaviours;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

public abstract class LocalRealmSupport extends RealmSupport implements RDFObject {
	private static String PROTOCOL = "1.1";
	private static String localhost;
	static {
		try {
			localhost = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			localhost = "localhost";
		}
	}
	private static String VIA = PROTOCOL + " " + localhost;

	@Override
	public Object authenticateAgent(String method, String via, Set<String> names,
			String algorithm, byte[] encoded) throws RepositoryException {
		if (via != null && via.endsWith(VIA))
			return getLocalhost();
		return null;
	}

	@Override
	public Object authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException {
		String[] via = request.get("via");
		if (via == null || via.length < 1)
			return null;
		if (via[via.length - 1].endsWith(VIA))
			return getLocalhost();
		return null;
	}

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, String qs) {
		return getLocalhost().equals(credential);
	}

	private RDFObject getLocalhost() {
		ObjectFactory of = getObjectConnection().getObjectFactory();
		return of.createObject("dns:" + localhost);
	}

}
