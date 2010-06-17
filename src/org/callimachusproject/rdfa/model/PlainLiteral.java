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

public abstract class PlainLiteral extends VarOrTermBase implements Literal {

	public abstract String getLang();

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('"').append(stringValue().replace("\"", "\\\"")).append('"');
		if (getLang() != null) {
			sb.append("@").append(getLang());
		}
		return sb.toString();
	}

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
		if (!(obj instanceof PlainLiteral))
			return false;
		PlainLiteral other = (PlainLiteral) obj;
		if (stringValue() == null) {
			if (other.stringValue() != null)
				return false;
		} else if (!stringValue().equals(other.stringValue()))
			return false;
		if (getLang() == null) {
			if (other.getLang() != null)
				return false;
		} else if (!getLang().equals(other.getLang()))
			return false;
		return true;
	}

}
