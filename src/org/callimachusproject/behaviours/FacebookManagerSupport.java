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
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.List;

import javax.tools.FileObject;

import org.callimachusproject.auth.CookieAuthenticationManager;
import org.callimachusproject.auth.DetachedAuthenticationManager;
import org.callimachusproject.auth.FacebookAuthReader;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.FacebookManager;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.RDFObject;

public abstract class FacebookManagerSupport extends
		AuthenticationManagerSupport implements RDFObject, FacebookManager {

	@Override
	public DetachedAuthenticationManager detachAuthenticationManager(
			String path, List<String> domains, RealmManager manager)
			throws OpenRDFException, IOException {
		String uri = this.getResource().stringValue();
		String url = uri + "?login";
		String reg = uri + "?register&";
		String appId = getCalliFacebookAppId();
		FileObject secretFile = getCalliFacebookSecret();
		if (secretFile == null)
			return null;
		CharSequence secret = secretFile.getCharContent(false);
		return new CookieAuthenticationManager(uri, url, reg, path, domains,
				manager, new FacebookAuthReader(uri, appId, secret, getHttpClient()));
	}

}
