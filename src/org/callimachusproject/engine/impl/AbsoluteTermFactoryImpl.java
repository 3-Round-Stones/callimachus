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
package org.callimachusproject.engine.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;

import org.callimachusproject.engine.model.CURIE;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.PlainLiteral;
import org.callimachusproject.engine.model.Reference;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.Literal;
import org.callimachusproject.engine.model.Var;

/**
 * Contains factory methods for RDF terms.
 * 
 * @author James Leigh
 *
 */
public class AbsoluteTermFactoryImpl extends AbsoluteTermFactory {
	private static final String prefix = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private static final CURIE XMLLiteral = new CURIEImpl(RDF.NAMESPACE, "XMLLiteral", "rdf");
	private static final CURIE STRING = new CURIEImpl(XMLSchema.NAMESPACE, "string", "xsd");
	private static final CURIE LANGSTRING = new CURIEImpl(RDF.NAMESPACE, "LangString", "rdf");

	@Override
	public CURIE curie(String ns, String reference, String prefix) {
		if (ns == null || reference == null || prefix == null)
			throw new IllegalArgumentException();
		return new CURIEImpl(ns, reference, prefix);
	}

	@Override
	public GraphNodePathImpl path(String path) {
		if (path == null)
			throw new IllegalArgumentException();
		return new GraphNodePathImpl(path);
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
		if (lang == null || lang.length() == 0)
			return new PlainLiteralImpl(label, STRING);
		return new PlainLiteralImpl(label, LANGSTRING, lang);
	}

	@Override
	public Literal literal(String label, IRI datatype) {
		if (label == null)
			throw new IllegalArgumentException();
		if (XMLLiteral.equals(datatype))
			return new XMLLiteralImpl(label, datatype);
		return new TypedLiteralImpl(label, datatype);
	}

	@Override
	public Var var(String name) {
		return var('?', name);
	}

	@Override
	public Var var(char prefix, String name) {
		if (name == null)
			throw new IllegalArgumentException();
		return new VarImpl(prefix, name);
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
