package org.callimachusproject.behaviours;


public abstract class NobodyRealmSupport extends RealmSupport {

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, String qs) {
		return false;
	}

}
