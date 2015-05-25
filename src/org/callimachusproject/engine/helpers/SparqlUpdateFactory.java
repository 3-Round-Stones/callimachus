/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved

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
package org.callimachusproject.engine.helpers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.clerezza.rdf.core.BNode;
import org.callimachusproject.engine.events.DeleteWhere;
import org.callimachusproject.engine.events.Document;
import org.callimachusproject.engine.events.InsertData;
import org.callimachusproject.engine.events.Namespace;
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.Term;
import org.callimachusproject.engine.model.VarOrTerm;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;

public class SparqlUpdateFactory {
	private final AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
	private final String base;

	public SparqlUpdateFactory(String base) {
		this.base = base;
	}

	public String replacement(GraphQueryResult deleteData,
			GraphQueryResult insertData) throws QueryEvaluationException,
			IOException {
		Collection<Statement> oldTriples = slurp(deleteData);
		Collection<Statement> newTriples = slurp(insertData);
		Collection<Statement> removed = copyMissing(oldTriples, newTriples);
		Collection<Statement> added = copyMissing(newTriples, oldTriples);
		Map<String, String> ns = new LinkedHashMap<String, String>();
		ns.putAll(deleteData.getNamespaces());
		ns.putAll(insertData.getNamespaces());
		return serialize(ns, removed, added);
	}

	public String replacement(Model deleteData, Model insertData)
			throws QueryEvaluationException, IOException {
		Collection<Statement> removed = copyMissing(deleteData, insertData);
		Collection<Statement> added = copyMissing(insertData, deleteData);
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (org.openrdf.model.Namespace ns : deleteData.getNamespaces()) {
			map.put(ns.getPrefix(), ns.getName());
		}
		for (org.openrdf.model.Namespace ns : insertData.getNamespaces()) {
			map.put(ns.getPrefix(), ns.getName());
		}
		return serialize(map, removed, added);
	}

	private Collection<Statement> slurp(GraphQueryResult data)
			throws QueryEvaluationException {
		Collection<Statement> set = new HashSet<Statement>();
		while (data.hasNext()) {
			Statement st = data.next();
			if (st.getContext() != null
					&& !st.getContext().stringValue().equals(base))
				throw new BadRequest("Only the default graph can be used, not "
						+ st.getContext());
			set.add(new StatementImpl(st.getSubject(), st.getPredicate(), st
					.getObject()));
		}
		return set;
	}

	private Collection<Statement> copyMissing(Collection<Statement> source,
			Collection<Statement> compare) {
		Collection<Statement> target = new HashSet<Statement>(source.size());
		for (Statement st : source) {
			if (!compare.contains(st) || st.getSubject() instanceof BNode
					|| st.getObject() instanceof BNode) {
				target.add(st);
			}
		}
		return target;
	}

	private String serialize(Map<String, String> ns,
			Collection<Statement> removed, Collection<Statement> added)
			throws IOException {
		StringWriter str = new StringWriter();
		SPARQLWriter writer = new SPARQLWriter(str);
		writer.write(new Document(true, null));
		for (Map.Entry<String, String> e : ns.entrySet()) {
			writer.write(new Namespace(e.getKey(), e.getValue()));
		}
		writer.write(new DeleteWhere(true, null));
		for (Statement st : removed) {
			Resource subj = st.getSubject();
			URI pred = st.getPredicate();
			Value obj = st.getObject();
			writer.write(new TriplePattern(asVarOrTerm(subj),
					asVarOrTerm(pred), asVarOrTerm(obj)));
		}
		writer.write(new DeleteWhere(false, null));
		writer.write(new InsertData(true, null));
		for (Statement st : added) {
			Resource subj = st.getSubject();
			URI pred = st.getPredicate();
			Value obj = st.getObject();
			writer.write(new Triple((Node) asTerm(subj), (IRI) asTerm(pred),
					asTerm(obj)));
		}
		writer.write(new InsertData(false, null));
		writer.write(new Document(false, null));
		writer.close();
		return str.toString();
	}

	private VarOrTerm asVarOrTerm(Value obj) {
		if (obj instanceof Literal) {
			Literal lit = (Literal) obj;
			if (lit.getDatatype() != null) {
				return tf.literal(obj.stringValue(),
						tf.iri(lit.getDatatype().stringValue()));
			} else if (lit.getLanguage() != null) {
				return tf.literal(obj.stringValue(), lit.getLanguage());
			} else {
				return tf.literal(obj.stringValue());
			}
		} else if (obj instanceof URI) {
			return tf.iri(obj.stringValue());
		} else {
			return tf.var("v" + obj.stringValue());
		}
	}

	private Term asTerm(Value obj) {
		if (obj instanceof Literal) {
			Literal lit = (Literal) obj;
			if (lit.getDatatype() != null) {
				return tf.literal(obj.stringValue(),
						tf.iri(lit.getDatatype().stringValue()));
			} else if (lit.getLanguage() != null) {
				return tf.literal(obj.stringValue(), lit.getLanguage());
			} else {
				return tf.literal(obj.stringValue());
			}
		} else if (obj instanceof URI) {
			return tf.iri(obj.stringValue());
		} else {
			return tf.node(obj.stringValue());
		}
	}
}
