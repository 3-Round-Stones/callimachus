package org.callimachusproject.repository;

import org.openrdf.query.algebra.evaluation.federation.SPARQLFederatedService;

public class SPARQLCredentialService extends SPARQLFederatedService {

	public SPARQLCredentialService(String serviceUrl) {
		super(serviceUrl);
	}

	public SPARQLCredentialService(String serviceUrl, String username, String password) {
		super(serviceUrl);
		rep.setUsernameAndPassword(username, password);
	}

	@Override
	protected void finalize() throws Throwable {
		this.shutdown();
		super.finalize();
	}

}
