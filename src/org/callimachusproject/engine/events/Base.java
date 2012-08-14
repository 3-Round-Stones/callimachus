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
package org.callimachusproject.engine.events;

import info.aduna.net.ParsedURI;

import javax.xml.stream.Location;

import org.callimachusproject.engine.model.TermFactory;

/**
 * Establishes the base URI of the document.
 * 
 * @author James Leigh
 *
 */
public class Base extends RDFEvent {
	private String base;
	private TermFactory tf;

	public Base(String base) {
		this(base, null);
	}

	public Base(String base, Location location) {
		super(location);
		assert base != null;
		this.tf = TermFactory.newInstance(base);
		this.base = tf.resolve(base);
	}

	public String getBase() {
		return base;
	}

	public String getReference() {
		String fragment = new ParsedURI(base).getFragment();
		if (fragment == null)
			return "";
		return "#" + fragment;
	}

	public String resolve(String relative) {
		return tf.resolve(relative);
	}

	public String toString() {
		return "BASE <" + getBase() + ">";
	}

	@Override
	public int hashCode() {
		return getBase().hashCode();
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
