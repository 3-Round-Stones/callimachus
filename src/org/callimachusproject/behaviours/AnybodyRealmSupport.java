package org.callimachusproject.behaviours;

import org.openrdf.repository.object.RDFObject;

public abstract class AnybodyRealmSupport extends RealmSupport implements RDFObject {

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, String qs) {
		return true;
	}

}
