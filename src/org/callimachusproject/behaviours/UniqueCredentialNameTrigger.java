/*
 * Copyright (c) 2010, Zepheira LLC and James Leigh Some rights reserved.
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
import org.openrdf.repository.object.annotations.sparql;
import org.openrdf.repository.object.annotations.triggeredBy;

/**
 * Ensures calli:name is unique across other groups and realms when a new membor
 * or group is added.
 * 
 * @author James Leigh
 * 
 */
public abstract class UniqueCredentialNameTrigger implements RDFObject {
	private final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";

	@triggeredBy("http://callimachusproject.org/rdf/2009/framework#calli:authenticates")
	public void checkCredentialNames() {
		if (isDuplicateCredentialName())
			throw new IllegalStateException("Username Already Exists");
	}

	@triggeredBy("http://callimachusproject.org/rdf/2009/framework#member")
	public void checkMemberNames() {
		if (isDuplicateMemberName())
			throw new IllegalStateException("Username Already Exists");
	}

	@sparql(PREFIX
			+ "ASK { $this calli:authenticates [calli:member ?credential1], [calli:member ?credential2] .\n"
			+ "?credential1 calli:name ?name . ?credential2 calli:name ?name\n"
			+ "FILTER (?credential1 != ?credential2) }")
	protected abstract boolean isDuplicateCredentialName();

	@sparql(PREFIX
			+ "ASK { ?realm calli:authenticates $this, [calli:member ?credential1].\n"
			+ "$this calli:member ?credential2 .\n"
			+ "?credential1 calli:name ?name . ?credential2 calli:name ?name\n"
			+ "FILTER (?credential1 != ?credential2) }")
	protected abstract boolean isDuplicateMemberName();
}
