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

import org.callimachusproject.rdfa.impl.TermFactoryImpl;

/**
 * Factory class for RDF terms.
 * 
 * @author James Leigh
 *
 */
public abstract class TermFactory {
	public static TermFactory newInstance() {
		return new TermFactoryImpl();
	}
	
	public abstract Var var(String name);

	public abstract IRI iri(String iri);

	public abstract Reference reference(String absolute, String relative);

	public abstract CURIE curie(String ns, String reference, String prefix);

	public abstract PlainLiteral literal(String label, String lang);

	public abstract TypedLiteral literal(String label, IRI datatype);

	public abstract Node node(String id);

	public abstract Node node();

	public PlainLiteral literal(String label) {
		return literal(label, (String) null);
	}
}
