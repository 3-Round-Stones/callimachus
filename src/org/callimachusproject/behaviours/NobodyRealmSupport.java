package org.callimachusproject.behaviours;

import java.util.Map;
import java.util.Set;

import org.openrdf.repository.RepositoryException;

public abstract class NobodyRealmSupport extends RealmSupport {

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
