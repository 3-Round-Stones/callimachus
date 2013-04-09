package org.callimachusproject.behaviours;

import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.concepts.Party;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class PartySupport implements Party, RDFObject {
	private final AuthorizationService service = AuthorizationService.getInstance();

	public void resetCache() throws RepositoryException {
		ObjectConnection conn = this.getObjectConnection();
        conn.commit();
		ObjectRepository repo = conn.getRepository();
		service.get(repo).resetCache();
	}

}
