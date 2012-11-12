package org.callimachusproject.auth;

import org.openrdf.repository.object.ObjectRepository;

public class AuthorizationFactory {

	public AuthorizationManager createAuthorizationManager(
			ObjectRepository repository) {
		return new AuthorizationManager(repository);
	}

}
