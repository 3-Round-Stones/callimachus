package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.Realm;
import org.callimachusproject.server.WebServer;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryException;

public abstract class RealmSupport implements CalliObject, Realm {

	/**
	 * Called from realm.ttl on logout
	 */
	public HttpResponse logout(Collection<String> tokens,
			String logoutContinue) throws IOException,
			OpenRDFException {
		DetachedRealm realm = this.getRealm();
		return realm.logout(tokens, logoutContinue);
	}

	public void resetCache() throws RepositoryException {
		this.getObjectConnection().commit();
		getCalliRepository().resetCache();
		WebServer.resetAllCache();
	}

	public String getPreferredManagerCookie(String manager) {
		String uri = getResource().stringValue();
		return DetachedRealm.getPreferredManagerCookie(uri, manager);
	}

	@Override
	public DetachedRealm detachRealm(RealmManager manager) {
		return new DetachedRealm(this.getResource());
	}

}
