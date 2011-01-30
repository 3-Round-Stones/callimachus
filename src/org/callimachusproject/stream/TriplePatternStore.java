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
package org.callimachusproject.stream;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Base;
import org.callimachusproject.rdfa.events.Construct;
import org.callimachusproject.rdfa.events.Document;
import org.callimachusproject.rdfa.events.Namespace;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.events.Where;
import org.callimachusproject.rdfa.model.VarOrIRI;
import org.callimachusproject.rdfa.model.VarOrTerm;

/**
 * Indexes basic graph patterns from a where clause and produces construct
 * queries from chosen patterns.
 * 
 * @author James Leigh
 */
public class TriplePatternStore {
	private Base base;
	private List<Namespace> namespaces = new ArrayList<Namespace>();
	private List<RDFEvent> where = new LinkedList<RDFEvent>();
	private TriplePattern firstTriplePattern;
	private Map<VarOrTerm, List<RDFEvent>> patterns = new HashMap<VarOrTerm, List<RDFEvent>>();
	private Map<VarOrIRI, List<TriplePattern>> triples = new HashMap<VarOrIRI, List<TriplePattern>>();

	public TriplePatternStore(String base) {
		this.base = new Base(base);
	}

	public String getReference() {
		return base.getReference();
	}

	public String resolve(String relative) {
		return base.resolve(relative);
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
		return tp;
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

	protected List<TriplePattern> getConstructPatterns(List<RDFEvent> where) {
		List<TriplePattern> list = new ArrayList<TriplePattern>(where.size());
		for (RDFEvent event : where) {
			if (event.isTriplePattern()) {
				TriplePattern tp = event.asTriplePattern();
				list.add(getProjectedPattern(tp));
			}
		}
		return list;
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
		for (TriplePattern event : getConstructPatterns(where)) {
			list.add(event);
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

}
