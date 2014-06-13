/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
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
package org.callimachusproject.engine.events;

import javax.xml.stream.Location;

import org.callimachusproject.engine.model.GraphNodePath;
import org.callimachusproject.engine.model.VarOrTerm;

/**
 * A basic graph pattern.
 * 
 * @author James Leigh
 *
 */
public class TriplePattern extends RDFEvent {
	private VarOrTerm subject;
	private GraphNodePath property;
	private VarOrTerm object;
	private boolean inverse;

	public TriplePattern(VarOrTerm subject, GraphNodePath property, VarOrTerm object) {
		this(subject, property, object, false, null);
	}

	public TriplePattern(VarOrTerm subject, GraphNodePath property, VarOrTerm object, Location location) {
		this(subject, property, object, false, location);
	}

	public TriplePattern(VarOrTerm subject, GraphNodePath property,
			VarOrTerm object, boolean inverse, Location location) {
		super(location);
		this.subject = subject;	
		this.property = property;
		this.object = object;
		this.inverse = inverse;
	}

	public VarOrTerm getSubject() {
		return subject;
	}

	public GraphNodePath getProperty() {
		return property;
	}

	public VarOrTerm getObject() {
		return object;
	}

	public boolean isInverse() {
		return inverse;
	}

	public VarOrTerm getAbout() {
		if (isInverse())
			return getObject();
		return getSubject();
	}

	public VarOrTerm getPartner() {
		if (isInverse())
			return getSubject();
		return getObject();
	}

	public String toString() {
		return subject.toString() + " " + property.toString() + " "
				+ object.toString();
	}

	@Override
	public int hashCode() {
		return 961 * subject.hashCode() + 31 * property.hashCode() + object.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TriplePattern other = (TriplePattern) obj;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (inverse != other.inverse)
			return false;
		return true;
	}

}
