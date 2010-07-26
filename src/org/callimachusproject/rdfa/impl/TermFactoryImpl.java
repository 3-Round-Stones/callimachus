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
package org.callimachusproject.rdfa.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.vocabulary.RDF;

import org.callimachusproject.rdfa.model.CURIE;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.Node;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.Reference;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.TypedLiteral;
import org.callimachusproject.rdfa.model.Var;

public class TermFactoryImpl extends TermFactory {
	private static final String prefix = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private static CURIE XMLLiteral = new CURIEImpl(RDF.NAMESPACE, "XMLLiteral", "rdf");

	@Override
	public CURIE curie(String ns, String reference, String prefix) {
		if (ns == null || reference == null || prefix == null)
			throw new IllegalArgumentException();
		return new CURIEImpl(ns, reference, prefix);
	}

	@Override
	public IRI iri(String iri) {
		if (iri == null)
			throw new IllegalArgumentException();
		return new IRIImpl(iri);
	}

	@Override
	public PlainLiteral literal(String label, String lang) {
		if (label == null)
			throw new IllegalArgumentException();
		return new PlainLiteralImpl(label, lang);
	}

	@Override
	public TypedLiteral literal(String label, IRI datatype) {
		if (label == null)
			throw new IllegalArgumentException();
		if (XMLLiteral.equals(datatype))
			return new XMLLiteralImpl(label, datatype);
		return new TypedLiteralImpl(label, datatype);
	}

	@Override
	public Var var(String name) {
		if (name == null)
			throw new IllegalArgumentException();
		return new VarImpl(name);
	}

	@Override
	public Node node(String id) {
		if (id == null)
			throw new IllegalArgumentException();
		return new BlankNode(id);
	}

	@Override
	public Node node() {
		return new BlankNode(prefix + seq.getAndIncrement());
	}

	@Override
	public Reference reference(String absolute, String relative) {
		if (absolute == null || relative == null)
			throw new IllegalArgumentException();
		return new ReferenceImpl(absolute, relative);
	}

}
