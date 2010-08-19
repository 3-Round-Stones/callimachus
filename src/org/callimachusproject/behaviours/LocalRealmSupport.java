package org.callimachusproject.behaviours;

import org.openrdf.repository.object.RDFObject;

public abstract class LocalRealmSupport extends RealmSupport implements RDFObject {

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, String qs) {
		String via = credential.toString();
		if (via.startsWith("dns:")) {
			return isLocal(via.substring("dns:".length()));
		}
		return false;
	}

}
