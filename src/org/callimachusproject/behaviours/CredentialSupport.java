package org.callimachusproject.behaviours;

import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.setup.SecretOriginProvider;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

public abstract class CredentialSupport implements RDFObject {

	public Object createSecretObject() throws RepositoryException {
		ObjectConnection con = this.getObjectConnection();
		String app = CalliRepository.getCallimachusWebapp(this.toString(), con);
		URI file = SecretOriginProvider.createSecretFile(app, con);
		return con.getObject(file);
	}
}
