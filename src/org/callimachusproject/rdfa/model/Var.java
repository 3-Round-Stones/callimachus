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
package org.callimachusproject.rdfa.model;

/**
 * SPARQL variable.
 * 
 * @author James Leigh
 *
 */
public abstract class Var extends VarOrTermBase implements VarOrIRI {

	@Override
	public int hashCode() {
		return stringValue().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Var))
			return false;
		Var other = (Var) obj;
		if (stringValue() == null) {
			if (other.stringValue() != null)
				return false;
		} else if (!stringValue().equals(other.stringValue()))
			return false;
		return true;
	}

	public String toString() {
		return "?" + stringValue();
	}
}
