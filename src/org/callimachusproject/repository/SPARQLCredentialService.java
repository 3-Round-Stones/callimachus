/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
