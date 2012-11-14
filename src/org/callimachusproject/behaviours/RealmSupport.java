package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.Realm;
import org.callimachusproject.server.HTTPObjectServer;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class RealmSupport implements RDFObject, Realm {
	private final AuthorizationService service = AuthorizationService.getInstance();

	/**
	 * Called from realm.ttl on logout
	 */
	public HttpResponse logout(Collection<String> tokens,
			String logoutContinue) throws IOException,
			OpenRDFException {
		String uri = this.getResource().stringValue();
		ObjectRepository repo = this.getObjectConnection().getRepository();
		DetachedRealm realm = service.get(repo).getRealm(uri);
		return realm.logout(tokens, logoutContinue);
	}

	public void resetCache() throws RepositoryException {
		ObjectConnection conn = this.getObjectConnection();
        conn.commit();
		ObjectRepository repo = conn.getRepository();
		service.get(repo).resetCache();
		HTTPObjectServer.resetAllCache();
	}

	public String getPreferredManagerCookie(String manager) {
		String uri = getResource().stringValue();
		return DetachedRealm.getPreferredManagerCookie(uri, manager);
	}

	@Override
	public DetachedRealm detachRealm(RealmManager manager) throws OpenRDFException {
		return new DetachedRealm(this.getResource(), this.getObjectConnection(), manager);
	}

}
