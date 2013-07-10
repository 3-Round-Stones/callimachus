package org.callimachusproject.behaviours;

import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.WebServer;
import org.callimachusproject.setup.SecretOriginProvider;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public abstract class CredentialSupport implements CalliObject {

	public Object createSecretObject() throws RepositoryException {
		ObjectConnection con = this.getObjectConnection();
		String app = CalliRepository.getCallimachusWebapp(this.toString(), con);
		URI file = SecretOriginProvider.createSecretFile(app, con);
		return con.getObject(file);
	}

	public void resetCache() throws RepositoryException {
		this.getObjectConnection().commit();
		getCalliRepository().resetCache();
		WebServer.resetAllCache();
	}
}
