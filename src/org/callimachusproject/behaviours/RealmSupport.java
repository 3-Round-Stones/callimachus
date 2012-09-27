package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.callimachusproject.server.auth.AuthorizationManager;
import org.callimachusproject.server.auth.Realm;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.RDFObject;

public abstract class RealmSupport implements RDFObject {
	private final AuthorizationManager manager = AuthorizationManager.getInstance();

	/**
	 * Called from realm.ttl on logout
	 */
	public HttpResponse logout(Collection<String> tokens,
			String logoutContinue) throws IOException,
			OpenRDFException {
		String uri = this.getResource().stringValue();
		Realm realm = manager.getRealm(uri, this.getObjectConnection());
		return realm.logout(tokens, logoutContinue);
	}

}
