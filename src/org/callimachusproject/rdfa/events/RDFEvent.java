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

public class RDFEvent {
	private Boolean start;

	public RDFEvent() {
		this.start = null;
	}

	public RDFEvent(boolean start) {
		this.start = start;
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

	public boolean isStartWhere() {
		return isStart() && this instanceof Where;
	}

	public boolean isEndWhere() {
		return isEnd() && this instanceof Where;
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

	public boolean isFilter() {
		return this instanceof Filter;
	}

	public boolean isConditionalOrExpression() {
		return this instanceof ConditionalOrExpression;
	}

	public boolean isStartBuiltInCall() {
		return isStart() && this instanceof BuiltInCall;
	}

	public boolean isEndBuiltInCall() {
		return isEnd() && this instanceof BuiltInCall;
	}

	public boolean isExpression() {
		return this instanceof Expression;
	}

	public Base asBase() {
		return (Base) this;
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

	public Expression asExpression() {
		return (Expression) this;
	}
}
