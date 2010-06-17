/*
   Copyright 2009 Zepheira LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.rdfa.events;

import org.callimachusproject.rdfa.model.VarOrIRI;
import org.callimachusproject.rdfa.model.VarOrTerm;

public class TriplePattern extends RDFEvent {
	private VarOrTerm subject;
	private VarOrIRI predicate;
	private VarOrTerm object;

	public TriplePattern(VarOrTerm subject, VarOrIRI predicate, VarOrTerm object) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public VarOrTerm getSubject() {
		return subject;
	}

	public VarOrIRI getPredicate() {
		return predicate;
	}

	public VarOrTerm getObject() {
		return object;
	}

	public String toString() {
		return subject.toString() + " " + predicate.toString() + " "
				+ object.toString();
	}

	@Override
	public int hashCode() {
		return 961 * subject.hashCode() + 31 * predicate.hashCode() + object.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TriplePattern))
			return false;
		TriplePattern other = (TriplePattern) obj;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}

}
