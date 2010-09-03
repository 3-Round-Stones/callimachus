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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Base;
import org.callimachusproject.rdfa.events.Construct;
import org.callimachusproject.rdfa.events.Document;
import org.callimachusproject.rdfa.events.Namespace;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.events.Where;
import org.callimachusproject.rdfa.model.Reference;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrIRI;
import org.callimachusproject.rdfa.model.VarOrTerm;

/**
 * Indexes basic graph patterns from a where clause and produces construct
 * queries from chosen patterns.
 * 
 * @author James Leigh
 */
public class TriplePatternStore {
	private static final String v = "http://callimachusproject.org/rdf/2009/framework/variables/?";
	private TermFactory tf = TermFactory.newInstance();
	private Base base;
	private List<Namespace> namespaces = new ArrayList<Namespace>();
	private List<RDFEvent> where = new LinkedList<RDFEvent>();
	private TriplePattern firstTriplePattern;
	private Map<VarOrTerm, List<RDFEvent>> patterns = new HashMap<VarOrTerm, List<RDFEvent>>();
	private Map<VarOrIRI, List<TriplePattern>> triples = new HashMap<VarOrIRI, List<TriplePattern>>();

	public TriplePatternStore(String base) {
		this.base = new Base(base);
	}

	public void consume(RDFEventReader reader) throws RDFParseException {
		Map<Integer, List<RDFEvent>> active = new HashMap<Integer, List<RDFEvent>>();
		active.put(0, where);
		int depth = 0;
		while (reader.hasNext()) {
			RDFEvent event = reader.next();
			if (event.isBase()) {
				base = event.asBase();
			} else if (event.isNamespace()) {
				namespaces.add(event.asNamespace());
			} else if (event.isStartSubject()) {
				depth++;
				VarOrTerm subj = event.asSubject().getSubject();
				List<RDFEvent> list = patterns.get(subj);
				if (list == null) {
					patterns.put(subj, list = new LinkedList<RDFEvent>());
				}
				active.put(depth, list);
				for (List<RDFEvent> l : active.values()) {
					l.add(event);
				}
			} else if (event.isEndSubject()) {
				for (List<RDFEvent> l : active.values()) {
					l.add(event);
				}
				active.remove(depth);
				depth--;
			} else if (event.isStartOptional() || event.isEndOptional()
					|| event.isFilter()) {
				for (List<RDFEvent> l : active.values()) {
					l.add(event);
				}
			} else if (event.isTriplePattern()) {
				if (firstTriplePattern == null) {
					firstTriplePattern = event.asTriplePattern();
				}
				for (List<RDFEvent> l : active.values()) {
					l.add(event);
				}
				TriplePattern tp = event.asTriplePattern();
				List<TriplePattern> list = triples.get(tp.getPredicate());
				if (list == null) {
					triples.put(tp.getPredicate(),
							list = new ArrayList<TriplePattern>());
				}
				list.add(tp);
			} else if (!event.isStartDocument() && !event.isEndDocument()) {
				for (List<RDFEvent> l : active.values()) {
					l.add(event);
				}
			}
		}
	}

	public TriplePattern getFirstTriplePattern() {
		return firstTriplePattern;
	}

	public List<TriplePattern> getPatternsByPredicate(VarOrIRI pred) {
		if (triples.containsKey(pred))
			return triples.get(pred);
		return Collections.emptyList();
	}

	public TriplePattern getProjectedPattern(TriplePattern tp) {
		VarOrTerm subj = tp.getSubject();
		VarOrIRI pred = tp.getPredicate();
		VarOrTerm obj = tp.getObject();
		if (tp.isInverse() && subj.isVar() && !(subj instanceof BlankOrLiteralVar)) {
			pred = tf.iri(v + subj.stringValue());
		} else if (!tp.isInverse() && obj.isVar() && !(obj instanceof BlankOrLiteralVar)) {
			pred = tf.iri(v + obj.stringValue());
		}
		return new TriplePattern(subj, pred, obj, tp.isInverse());
	}

	public RDFEventReader openQueryReader() {
		if (where.isEmpty())
			return null;
		return openQueryReader(where);
	}

	/**
	 * If the query starts off with OPIONAL blocks return prefixing OPTIONAL
	 * blocks as a separate queries. Otherwise just return one query.
	 */
	public List<RDFEventReader> openWellJoinedQueries() {
		if (where.isEmpty())
			return Collections.emptyList();
		List<List<RDFEvent>> list = new ArrayList<List<RDFEvent>>();
		List<RDFEvent> top = new ArrayList<RDFEvent>();
		List<RDFEvent> block = null;
		int level = 0;
		for (RDFEvent event : where) {
			if (event.isStartOptional()) {
				if (level == 0) {
					block = new ArrayList<RDFEvent>();
				} else {
					block.add(event);
				}
				level++;
			} else if (event.isEndOptional()) {
				level--;
				if (level == 0) {
					list.add(block);
					block = null;
				} else {
					block.add(event);
				}
			} else if (level == 0) {
				top.add(event);
			} else {
				block.add(event);
			}
		}
		if (list.isEmpty() && !top.isEmpty()) {
			return Collections.singletonList(openQueryReader(top));
		}
		List<RDFEventReader> result = new ArrayList<RDFEventReader>(list.size());
		for (List<RDFEvent> optional : list) {
			List<RDFEvent> qry = new ArrayList<RDFEvent>(top.size() + optional.size());
			qry.addAll(top);
			qry.addAll(optional);
			result.add(openQueryReader(qry));
		}
		return result ;
	}

	public RDFEventReader openQueryBySubject(VarOrTerm subj) {
		if (!patterns.containsKey(subj))
			return null;
		return openQueryReader(patterns.get(subj));
	}

	public String toString() {
		StringWriter str = new StringWriter();
		try {
			RDFEventReader reader = openQueryReader();
			if (reader == null)
				return "CONSTRUCT {}";
			try {
				SPARQLWriter writer = new SPARQLWriter(str);
				while (reader.hasNext()) {
					writer.write(reader.next());
				}
				return str.toString();
			} finally {
				reader.close();
			}
		} catch (Exception e) {
			return super.toString();
		}
	}

	private RDFEventReader openQueryReader(List<RDFEvent> where) {
		List<RDFEvent> list = new ArrayList<RDFEvent>();
		list.add(new Document(true));
		if (base != null) {
			list.add(base);
		}
		for (RDFEvent event : namespaces) {
			list.add(event);
		}
		list.add(new Construct(true));
		for (List<TriplePattern> patterns : getConstructPatterns(where)) {
			for (TriplePattern event : patterns) {
				list.add(event);
			}
		}
		list.add(new Construct(false));
		list.add(new Where(true));
		for (RDFEvent event : where) {
			list.add(event);
		}
		list.add(new Where(false));
		list.add(new Document(false));
		return new IterableRDFEventReader(list);
	}

	private Iterable<List<TriplePattern>> getConstructPatterns(
			List<RDFEvent> where) {
		Set<VarOrTerm> variables = new HashSet<VarOrTerm>();
		Map<VarOrTerm, List<TriplePattern>> construct = new LinkedHashMap<VarOrTerm, List<TriplePattern>>();
		for (RDFEvent event : where) {
			if (event.isTriplePattern()) {
				TriplePattern tp = event.asTriplePattern();
				VarOrTerm subj = tp.getSubject();
				VarOrTerm obj = tp.getObject();
				List<TriplePattern> list = construct.get(subj);
				if (list == null) {
					construct.put(subj, list = new LinkedList<TriplePattern>());
				}
				if (subj.isVar() && !(subj instanceof BlankOrLiteralVar) && !variables.contains(subj)) {
					VarOrIRI pred = tf.iri(v + subj.stringValue());
					if (tp.isInverse()) {
						list.add(new TriplePattern(obj, pred, subj));
					} else {
						Reference ref = tf.reference(base.resolve(""), "");
						list.add(new TriplePattern(ref, pred, subj));
					}
					variables.add(subj);
				} else if (tp.isInverse()) {
					list.add(new TriplePattern(obj, tp.getPredicate(), subj));
				}
				if (obj.isVar() && !(obj instanceof BlankOrLiteralVar)) {
					variables.add(obj);
				}
				list.add(getProjectedPattern(tp));
			}
		}
		return construct.values();
	}

}
