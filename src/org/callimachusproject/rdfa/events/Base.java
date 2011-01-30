/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

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

import info.aduna.net.ParsedURI;

/**
 * Establishes the base URI of the document.
 * 
 * @author James Leigh
 *
 */
public class Base extends RDFEvent {
	private ParsedURI base;

	public Base(String base) {
		assert base != null;
		this.base = new ParsedURI(base);
		assert this.base.isAbsolute();
	}

	public String getBase() {
		return base.toString();
	}

	public String getReference() {
		String fragment = base.getFragment();
		if (fragment == null)
			return "";
		return "#" + fragment;
	}

	public String resolve(String relative) {
		if (relative.startsWith("?")) {
			String iri = base.toString();
			if (!iri.contains("?") && !iri.contains("#")) {
				return iri + relative;
			}
		}
		return base.resolve(relative).toString();
	}

	public String toString() {
		return "BASE <" + getBase() + ">";
	}

	@Override
	public int hashCode() {
		return base.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Base other = (Base) obj;
		if (!base.equals(other.base))
			return false;
		return true;
	}

}
