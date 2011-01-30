/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
   Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved 
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

import org.callimachusproject.concepts.Group;
import org.openrdf.repository.object.annotations.sparql;
import org.openrdf.repository.object.annotations.triggeredBy;

/**
 * Ensures calli:name is unique across groups within the same realm when a new
 * member is added.
 * 
 * @author James Leigh
 * 
 */
public abstract class UniqueMemberNameTrigger implements Group {
	private static final String NS = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String PREFIX = "PREFIX calli:<" + NS + ">\n";

	@triggeredBy(NS + "member")
	public void checkMemberNames() {
		if (isDuplicateMemberName())
			throw new IllegalStateException("Username Already Exists");
	}

	@sparql(PREFIX
			+ "ASK { ?realm calli:authenticates $this, [calli:member ?credential1].\n"
			+ "$this calli:member ?credential2 .\n"
			+ "?credential1 calli:name ?name . ?credential2 calli:name ?name\n"
			+ "FILTER (?credential1 != ?credential2) }")
	protected abstract boolean isDuplicateMemberName();
}
