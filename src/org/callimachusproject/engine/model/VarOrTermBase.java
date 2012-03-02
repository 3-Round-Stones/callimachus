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
package org.callimachusproject.engine.model;

/**
 * Common methods for {@link VarOrTerm}.
 * 
 * @author James Leigh
 * 
 */
public abstract class VarOrTermBase implements VarOrTerm {
	private TermOrigin origin;

	public boolean isNode() {
		return this instanceof Node;
	}

	public boolean isIRI() {
		return this instanceof IRI;
	}

	public boolean isReference() {
		return this instanceof Reference;
	}

	public boolean isCURIE() {
		return this instanceof CURIE;
	}

	public boolean isLiteral() {
		return this instanceof Literal;
	}

	public boolean isPlainLiteral() {
		return this instanceof PlainLiteral;
	}

	public boolean isTypedLiteral() {
		return this instanceof TypedLiteral;
	}

	public boolean isXMLLiteral() {
		return this instanceof XMLLiteral;
	}

	public boolean isTerm() {
		return this instanceof Term;
	}

	public boolean isVar() {
		return this instanceof Var;
	}

	@Override
	public Var asVar() {
		return (Var) this;
	}

	public Reference asReference() {
		return (Reference) this;
	}

	public CURIE asCURIE() {
		return (CURIE) this;
	}

	public PlainLiteral asPlainLiteral() {
		return (PlainLiteral) this;
	}

	public TypedLiteral asTypedLiteral() {
		return (TypedLiteral) this;
	}

	public XMLLiteral asXMLLiteral() {
		return (XMLLiteral) this;
	}

	public abstract String stringValue();

	public void setOrigin(TermOrigin origin) {
		assert this.origin == null;
		this.origin = origin;
	}

	public TermOrigin getOrigin() {
		return origin;
	}

}
