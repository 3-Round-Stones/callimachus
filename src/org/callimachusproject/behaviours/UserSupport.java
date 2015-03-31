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

import org.apache.commons.codec.digest.DigestUtils;
import org.callimachusproject.concepts.User;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.BadRequest;

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
