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
package org.callimachusproject.auth;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;

public interface DigestAccessor {

	String getIdentifier();

	void registerUser(Resource invitedUser, URI registeredUser,
			ObjectConnection con) throws OpenRDFException, IOException;

	Map<String, String> findDigestUser(String method, String username,
			String authName, Collection<String> cookies, ObjectConnection con)
			throws OpenRDFException, IOException;

	HttpResponse getLogoutResponse();

	HttpResponse getBadCredentialResponse(String method, String url,
			String[] via, Collection<String> cookies)
			throws IOException;

	HttpResponse getNotLoggedInResponse(String method, String url,
			String[] via, Collection<String> cookies)
			throws IOException;

}
