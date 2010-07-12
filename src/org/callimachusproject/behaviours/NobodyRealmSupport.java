package org.callimachusproject.behaviours;

import java.util.Map;

import org.openrdf.repository.RepositoryException;

public abstract class NobodyRealmSupport extends RealmSupport {

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
