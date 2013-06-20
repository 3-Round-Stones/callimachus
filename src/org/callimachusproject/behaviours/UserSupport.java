package org.callimachusproject.behaviours;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.callimachusproject.concepts.User;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;

public abstract class UserSupport implements User, CalliObject {

	public String getSecretToken(String nonce) throws OpenRDFException,
			IOException {
		if (nonce == null || nonce.length() == 0)
			throw new BadRequest("Missing nonce");
		String uri = this.getResource().stringValue();
		String hash = DigestUtils.md5Hex(uri);
		String secret = this.getRealm().getOriginSecret();
		return DigestUtils.md5Hex(hash + ":" + nonce + ":" + secret);
	}
}
