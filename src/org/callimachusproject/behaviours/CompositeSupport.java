package org.callimachusproject.behaviours;

import org.callimachusproject.auth.AuthorizationService;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class CompositeSupport implements RDFObject {
	private final AuthorizationService service = AuthorizationService.getInstance();

	public boolean isAuthorized(String user, RDFObject target, String[] roles)
			throws RepositoryException, OpenRDFException {
		ObjectConnection conn = this.getObjectConnection();
		ObjectRepository repo = conn.getRepository();
		return service.get(repo).isAuthorized(user, target, roles);
	}

}
