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
package org.callimachusproject.concepts;

import java.util.Set;

import org.openrdf.annotations.Iri;

/** Credentials for a person. */
@Iri("http://callimachusproject.org/rdf/2009/framework#User")
public interface User {
	/** User's full name. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#label")
	String getCalliFullName();

	/** The primary email address for this user. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#email")
	String getCalliEmail();
	/** The primary email address for this user */
	@Iri("http://callimachusproject.org/rdf/2009/framework#email")
	void setCalliEmail(CharSequence calliEmail);

	/** The username for this agent */
	@Iri("http://callimachusproject.org/rdf/2009/framework#name")
	CharSequence getCalliName();
	/** The username for this agent */
	@Iri("http://callimachusproject.org/rdf/2009/framework#name")
	void setCalliName(CharSequence calliName);

	/** A document of the MD5 sum of email:authName:password in HEX encoding */
	@Iri("http://callimachusproject.org/rdf/2009/framework#passwordDigest")
	Set<Object> getCalliPasswordDigest();
	/** A document of the MD5 sum of email:authName:password in HEX encoding */
	@Iri("http://callimachusproject.org/rdf/2009/framework#passwordDigest")
	void setCalliPasswordDigest(Set<?> calliPasswordDigest);

}
