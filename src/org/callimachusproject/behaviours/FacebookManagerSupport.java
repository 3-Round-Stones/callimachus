/*
 * Copyright (c) 2012, James Leigh Some rights reserved.
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
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.auth.DetachedAuthenticationManager;
import org.callimachusproject.auth.DetachedFacebookManager;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.FacebookManager;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class FacebookManagerSupport implements RDFObject, FacebookManager {
	private final AuthorizationService service = AuthorizationService.getInstance();

	@Override
	public DetachedAuthenticationManager detachAuthenticationManager(String path,
			List<String> domains, RealmManager manager) throws OpenRDFException, IOException {
		String uri = this.getResource().stringValue();
		String url = uri + "?login";
		String appId = getCalliFacebookAppId();
		CharSequence secret = getCalliFacebookSecret().getCharContent(false);
		return new DetachedFacebookManager(uri, url, appId, secret, path);
	}

	public void resetCache() throws RepositoryException {
		ObjectConnection conn = this.getObjectConnection();
        conn.commit();
		ObjectRepository repo = conn.getRepository();
		service.get(repo).resetCache();
	}

	public void registerUser(Resource user, Collection<String> cookies)
			throws OpenRDFException, IOException {
		getManager().registerUser(user, cookies, this.getObjectConnection());
	}

	@Override
	public String getUserIdentifier(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedFacebookManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUserIdentifier(tokens);
	}

	@Override
	public String getUserLogin(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedFacebookManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUserLogin(tokens);
	}

	@Override
	public String getUsernameSetCookie(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedFacebookManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUsernameSetCookie(tokens);
	}

	private DetachedFacebookManager getManager() throws OpenRDFException, IOException {
		Resource self = this.getResource();
		ObjectRepository repo = this.getObjectConnection().getRepository();
		AuthorizationManager manager = service.get(repo);
		return (DetachedFacebookManager) manager.getAuthenticationManager(self);
	}

}
