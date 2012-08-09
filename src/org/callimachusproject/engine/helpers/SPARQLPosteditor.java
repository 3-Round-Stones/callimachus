/*
 * Portions Copyright (c) 2011 Talis Inc, Some Rights Reserved
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

package org.callimachusproject.engine.helpers;

/**
 * Edit an XHTML+RDFa event stream, conditionally add/remove events 
 * 
 * @author Steve Battle
 */

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.Location;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.BuiltInCall;
import org.callimachusproject.engine.events.Exists;
import org.callimachusproject.engine.events.Filter;
import org.callimachusproject.engine.events.Namespace;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.events.VarOrTermExpression;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.TermOrigin;
import org.callimachusproject.engine.model.Var;
import org.callimachusproject.engine.model.VarOrTerm;

public class SPARQLPosteditor extends RDFEventPipe {
	private static final String KEYWORD_NS = "http://www.openrdf.org/rdf/2011/keyword#";
	private static boolean OPEN = true, CLOSE = false;

	private static AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();

	List<Editor> editors = new LinkedList<Editor>();
	Map<String, TermOrigin> origins;
	
	interface Editor {
		boolean edit(RDFEvent event);
	}
	
	public class TriplePatternCutter implements Editor {
		public boolean edit(RDFEvent event) {
			if (!event.isTriplePattern()) return false;
			TriplePattern t = event.asTriplePattern();
			VarOrTerm vt = t.getPartner();
			if (vt==null || !vt.isVar())
				return false;
			Var v = vt.asVar();
			TermOrigin origin = origins.get(v.stringValue());
			if (origin.isAnchor())
				return true;
			return false;
		}
	}
	
	public class PhoneMatchInsert implements Editor {
		String keyword;
		boolean includesNamespace = false;
		public PhoneMatchInsert(String keyword) {
			super();
			this.keyword = keyword;
		}
		@Override
		public boolean edit(RDFEvent event) {
			Location location = event.getLocation();
			if (!includesNamespace && event.isNamespace()) {
				if (event.asNamespace().getPrefix().equals("keyword")) {
					includesNamespace = true;
				}
			} else if (!includesNamespace && !event.isStartDocument() && !event.isComment()) {
				add(new Namespace("keyword", KEYWORD_NS, location));
				includesNamespace = true;
			}
			if (!event.isEndSubject()) return false;
			VarOrTerm vt = event.asSubject().getSubject();
			if (vt.isVar() 
			&& match(vt)) {
				Var var = tf.var(vt.stringValue() + "_phone");
				add(new TriplePattern(vt,tf.curie(KEYWORD_NS, "phone", "keyword"),var, location));
				// eg. FILTER sameTerm(?vt,keyword:soundex(keyword))
				add(new Filter(OPEN, location));
				add(new BuiltInCall(OPEN, "sameTerm", location));
				add(new VarOrTermExpression(var, location));
				add(new BuiltInCall(OPEN, "keyword:soundex", location));
				add(new VarOrTermExpression(tf.literal(keyword), location));
				add(new BuiltInCall(CLOSE, "keyword:soundex", location));
				add(new BuiltInCall(CLOSE, "sameTerm", location));
				add(new Filter(CLOSE, location));
			}
			return false;
		}
	}
	
	public class FilterKeywordExists implements Editor {
		VarOrTerm subject, object;
		String keyword;
		public FilterKeywordExists(String keyword) {
			super();
			this.keyword = keyword;
		}
		@Override
		public boolean edit(RDFEvent event) {
			// the subject of the filter is the first matching subject
			if (event.isTriplePattern()) {
				TriplePattern t = event.asTriplePattern();
				if (subject==null && match(t.getSubject())) {
					subject = t.getAbout();
					object = tf.var("__label");
				}
			}
			// add the filter exists at the end, use "__" prefix for introduced variables
			// these variables are extraneous to the template (having no origin)
			else if (event.isEndWhere() && subject!=null) {
				Location location = event.getLocation();
				add(new Filter(OPEN, location));
				add(new Exists(OPEN, location));
				
				VarOrTerm subj = subject;
				Var pred = tf.var("__label_property");
				add(new TriplePattern(subj,pred,object, location));
				// eg. FILTER regex(?object,keyword:regex($keyword))
				add(new Filter(OPEN, location));
				add(new BuiltInCall(OPEN, "regex", location));
				add(new VarOrTermExpression(object, location));
				add(new BuiltInCall(OPEN, "keyword:regex", location));
				add(new VarOrTermExpression(tf.literal(keyword), location));
				add(new BuiltInCall(CLOSE, "keyword:regex", location));
				add(new BuiltInCall(CLOSE, "regex", location));
				add(new Filter(CLOSE, location));

				add(new Exists(CLOSE, location));
				add(new Filter(CLOSE, location));
			}
			return false;
		}		
	}
	
	public class TriplePatternRecorder implements Editor {
		List<TriplePattern> triples = new LinkedList<TriplePattern>();
		@Override
		public boolean edit(RDFEvent event) {
			if (!event.isTriplePattern()) return false;
			TriplePattern t = event.asTriplePattern();
			if (match(t.getSubject()))
				triples.add(t);
			return false;
		}
		public List<TriplePattern> getTriplePatterns() {
			return triples;
		}
	}
		
	private boolean match(VarOrTerm vt) {
		boolean matches = true;
		if (vt!=null && vt.isVar()) {
			Var v = vt.asVar();
			TermOrigin origin = origins.get(v.stringValue());
			matches = origin.isAnchor();
		}
		return matches;
	}	

	public SPARQLPosteditor(RDFEventReader reader, Map<String, TermOrigin> origins) throws RDFParseException {
		this(new RDFEventList(reader), origins);
	}	

	public SPARQLPosteditor(RDFEventList list, Map<String, TermOrigin> origins) {
		this(list, list.iterator(), origins);
	}	

	public SPARQLPosteditor(RDFEventList input, RDFEventIterator reader, Map<String, TermOrigin> origins) {
		super(reader);
		this.origins = origins;
	}
	
	public SPARQLPosteditor(SPARQLProducer producer) throws RDFParseException {
		this(producer,producer.getOrigins());
	}
	
	public void addEditor(Editor m) {
		editors.add(m);
	}

	/* Any triple matcher has the power to veto inclusion of the triple */
	
	@Override
	protected void process(RDFEvent event) throws RDFParseException {
		if (event==null) return;
		for (Editor ed: editors) {
			// if the edit returns true, then skip further editing
			// only required to cut or re-order events
			if (ed.edit(event)) return;
		}
		add(event);
	}

}
