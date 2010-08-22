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

import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.Node;
import org.callimachusproject.rdfa.model.Term;

public class Triple extends TriplePattern {

	public Triple(Node subject, IRI predicate, Term object) {
		super(subject, predicate, object);
	}

	public Triple(Node subject, IRI predicate, Term object,
			boolean inverse) {
		super(subject, predicate, object, inverse);
	}

	@Override
	public Node getSubject() {
		return (Node) super.getSubject();
	}

	@Override
	public IRI getPredicate() {
		return (IRI) super.getPredicate();
	}

	@Override
	public Term getObject() {
		return (Term) super.getObject();
	}

}
