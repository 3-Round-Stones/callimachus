package org.callimachusproject.behaviours;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.concepts.User;
import org.callimachusproject.server.exceptions.BadRequest;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class UserSupport implements User, RDFObject {
	private final AuthorizationService service = AuthorizationService.getInstance();

	public String getSecretToken(String nonce) throws OpenRDFException,
			IOException {
		if (nonce == null || nonce.length() == 0)
			throw new BadRequest("Missing nonce");
		String uri = this.getResource().stringValue();
		String hash = DigestUtils.md5Hex(uri);
		ObjectRepository repo = this.getObjectConnection().getRepository();
		AuthorizationManager manager = service.get(repo);
		String secret = manager.getRealm(uri).getOriginSecret();
		return DigestUtils.md5Hex(hash + ":" + nonce + ":" + secret);
	}
}
