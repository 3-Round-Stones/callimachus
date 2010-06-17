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

import org.callimachusproject.rdfa.model.VarOrTerm;

public class Expression extends Filter {
	private VarOrTerm term;

	public Expression(VarOrTerm term) {
		this.term = term;
	}

	public VarOrTerm getTerm() {
		return term;
	}

	@Override
	public String toString() {
		return term.toString();
	}

}
