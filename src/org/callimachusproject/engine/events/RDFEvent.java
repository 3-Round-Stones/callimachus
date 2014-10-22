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

import javax.xml.stream.Location;

import org.callimachusproject.engine.model.VarOrTerm;

/**
 * A basic RDF event that occurs within a document.
 * 
 * @author James Leigh
 * @author Steve Battle
 *
 */
public class RDFEvent {
	private final Boolean start;
	private final Location location;
	
	public RDFEvent(Location location) {
		this.start = null;
		this.location = location;
	}

	public RDFEvent(boolean start, Location location) {
		this.start = start;
		this.location = location;
	}

	/**
	 * Return the location of this event. The Location returned from this method
	 * is non-volatile and will retain its information.
	 * 
	 * @see javax.xml.stream.Location
	 */
	public Location getLocation() {
		return location;
	}

	public boolean isStart() {
		if (start == null)
			return false;
		return start;
	}

	public boolean isEnd() {
		if (start == null)
			return false;
		return !start;
	}

	public boolean isStartDocument() {
		return isStart() && this instanceof Document;
	}

	public boolean isEndDocument() {
		return isEnd() && this instanceof Document;
	}

	public boolean isNamespace() {
		return this instanceof Namespace;
	}

	public boolean isBase() {
		return this instanceof Base;
	}

	public boolean isStartConstruct() {
		return isStart() && this instanceof Construct;
	}

	public boolean isEndConstruct() {
		return isEnd() && this instanceof Construct;
	}

	public boolean isAsk() {
		return this instanceof Ask;
	}

	public boolean isSelect() {
		return this instanceof Select;
	}

	public boolean isStartWhere() {
		return isStart() && this instanceof Where;
	}

	public boolean isEndWhere() {
		return isEnd() && this instanceof Where;
	}

	public boolean isStartInsert() {
		return isStart() && this instanceof Insert;
	}

	public boolean isEndInsert() {
		return isEnd() && this instanceof Insert;
	}

	public boolean isStartDeleteWhere() {
		return isStart() && this instanceof DeleteWhere;
	}

	public boolean isEndDeleteWhere() {
		return isEnd() && this instanceof DeleteWhere;
	}

	public boolean isStartExists() {
		return isStart() && this instanceof Exists;
	}

	public boolean isEndExists() {
		return isEnd() && this instanceof Exists;
	}

	public boolean isStartGroup() {
		return isStart() && this instanceof Group;
	}

	public boolean isEndGroup() {
		return isEnd() && this instanceof Group;
	}

	public boolean isUnion() {
		return this instanceof Union;
	}

	public boolean isStartOptional() {
		return isStart() && this instanceof Optional;
	}

	public boolean isEndOptional() {
		return isEnd() && this instanceof Optional;
	}

	public boolean isStartGraph() {
		return isStart() && this instanceof Graph;
	}

	public boolean isEndGraph() {
		return isEnd() && this instanceof Graph;
	}

	public boolean isStartSubject() {
		return isStart() && this instanceof Subject;
	}

	public boolean isEndSubject() {
		return isEnd() && this instanceof Subject;
	}

	public boolean isTriple() {
		return this instanceof Triple;
	}

	public boolean isTriplePattern() {
		return this instanceof TriplePattern;
	}

	public boolean isStartFilter() {
		return this instanceof Filter && isStart();
	}
	
	public boolean isEndFilter() {
		return this instanceof Filter && isEnd();
	}

	public boolean isExpression() {
		return this instanceof Expression;
	}

	public boolean isStartBuiltInCall() {
		return isStart() && this instanceof BuiltInCall;
	}

	public boolean isEndBuiltInCall() {
		return isEnd() && this instanceof BuiltInCall;
	}

	public boolean isVarOrTerm() {
		return this instanceof VarOrTermExpression;
	}

	public boolean isComment() {
		return this instanceof Comment;
	}

	public Base asBase() {
		return (Base) this;
	}

	public Comment asComment() {
		return (Comment) this;
	}

	public Namespace asNamespace() {
		return (Namespace) this;
	}

	public Graph asGraph() {
		return (Graph) this;
	}

	public Subject asSubject() {
		return (Subject) this;
	}

	public Triple asTriple() {
		return (Triple) this;
	}

	public TriplePattern asTriplePattern() {
		return (TriplePattern) this;
	}

	public BuiltInCall asBuiltInCall() {
		return (BuiltInCall) this;
	}

	public VarOrTerm asVarOrTerm() {
		return ((VarOrTermExpression) this).getTerm();
	}
}
