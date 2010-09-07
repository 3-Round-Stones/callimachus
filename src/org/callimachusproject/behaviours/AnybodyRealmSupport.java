/*
 * Copyright 2010 Zepheira LLC
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

import org.openrdf.repository.object.RDFObject;

/**
 * Permits any client in this realm and records the external host name.
 * 
 * @author James Leigh
 *
 */
public abstract class AnybodyRealmSupport extends RealmSupport implements RDFObject {

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, String qs) {
		return true;
	}

}
