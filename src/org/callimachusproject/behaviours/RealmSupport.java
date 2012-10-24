package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.callimachusproject.server.HTTPObjectServer;
import org.callimachusproject.server.auth.AuthorizationService;
import org.callimachusproject.server.auth.Realm;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class RealmSupport implements RDFObject {
	private final AuthorizationService service = AuthorizationService.getInstance();

	/**
	 * Called from realm.ttl on logout
	 */
	public HttpResponse logout(Collection<String> tokens,
			String logoutContinue) throws IOException,
			OpenRDFException {
		String uri = this.getResource().stringValue();
		ObjectRepository repo = this.getObjectConnection().getRepository();
		Realm realm = service.get(repo).getRealm(uri);
		return realm.logout(tokens, logoutContinue);
	}

	public void resetCache() throws RepositoryException {
		ObjectConnection conn = this.getObjectConnection();
        conn.commit();
		ObjectRepository repo = conn.getRepository();
		service.get(repo).resetCache();
		HTTPObjectServer.resetAllCache();
	}

}
