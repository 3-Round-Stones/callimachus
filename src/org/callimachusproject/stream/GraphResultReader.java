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
package org.callimachusproject.stream;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Document;
import org.callimachusproject.rdfa.events.Graph;
import org.callimachusproject.rdfa.events.Namespace;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.Node;
import org.callimachusproject.rdfa.model.Term;
import org.callimachusproject.rdfa.model.TermFactory;

public class GraphResultReader extends RDFEventReader {
	private TermFactory tf = TermFactory.newInstance();
	private Queue<RDFEvent> queue = new LinkedList<RDFEvent>();
	private GraphQueryResult result;
	private Resource context;
	private boolean started;
	private boolean end;
	private Map<String, String> prefixes = new HashMap<String, String>();

	public GraphResultReader(GraphQueryResult result) {
		this.result = result;
	}

	public String toString() {
		return result.toString();
	}

	@Override
	public void close() throws RDFParseException {
		try {
			result.close();
		} catch (QueryEvaluationException e) {
			throw new RDFParseException(e);
		}
	}

	@Override
	protected RDFEvent take() throws RDFParseException {
		try {
			while (queue.isEmpty()) {
				if (end)
					return null;
				if (!started) {
					queue.add(new Document(true));
					Map<String, String> map = result.getNamespaces();
					for (Entry<String, String> e : map.entrySet()) {
						queue.add(new Namespace(e.getKey(), e.getValue()));
						prefixes.put(e.getValue(), e.getKey());
					}
					started = true;
					return queue.remove();
				}
				if (!result.hasNext()) {
					end = true;
					if (context != null && context instanceof URI) {
						queue.add(new Graph(false, asIRI((URI) context)));
					}
					context = null;
					queue.add(new Document(false));
					return queue.remove();
				}
				process(result.next());
			}
			return queue.remove();
		} catch (QueryEvaluationException e) {
			throw new RDFParseException(e);
		}
	}

	private void process(Statement st) throws QueryEvaluationException {
		if (!equals(context, st.getContext())) {
			if (context != null && context instanceof URI) {
				queue.add(new Graph(false, asIRI((URI) context)));
			}
			context = st.getContext();
			if (context != null && context instanceof URI) {
				queue.add(new Graph(true, asIRI((URI) context)));
			}
		}
		Node subj = asNode(st.getSubject());
		IRI pred = asIRI(st.getPredicate());
		Term obj = asTerm(st.getObject());
		queue.add(new Triple(subj, pred, obj));
	}

	private boolean equals(Resource o1, Resource o2) {
		if (o1 == o2)
			return true;
		if (o1 == null || o2 == null)
			return false;
		return o1.equals(o2);
	}

	private Node asNode(Resource subj) {
		if (subj instanceof URI)
			return asIRI((URI) subj);
		return tf.node(subj.stringValue());
	}

	private IRI asIRI(URI pred) {
		String ns = pred.getNamespace();
		if (prefixes.containsKey(ns))
			return tf.curie(ns, pred.getLocalName(), prefixes.get(ns));
		return tf.iri(pred.stringValue());
	}

	private Term asTerm(Value obj) {
		if (obj instanceof Resource)
			return asNode((Resource) obj);
		Literal lit = (Literal) obj;
		if (lit.getDatatype() == null)
			return tf.literal(lit.getLabel(), lit.getLanguage());
		return tf.literal(lit.getLabel(), asIRI(lit.getDatatype()));
	}

}
