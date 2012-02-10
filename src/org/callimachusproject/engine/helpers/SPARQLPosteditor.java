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
import java.util.regex.Pattern;

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
import org.callimachusproject.engine.model.Var;
import org.callimachusproject.engine.model.VarOrTerm;
import org.openrdf.model.URI;

public class SPARQLPosteditor extends BufferedRDFEventReader {
	private static final String KEYWORD_NS = "http://www.openrdf.org/rdf/2011/keyword#";
	private static boolean OPEN = true, CLOSE = false;

	private static AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();

	List<Editor> editors = new LinkedList<Editor>();
	Map<String, String> origins;
	
	interface Editor {
		boolean edit(RDFEvent event);
	}
	
	public class TriplePatternCutter implements Editor {
		Pattern about, partner;
		public TriplePatternCutter(String about, String partner) {
			if (about!=null) this.about = Pattern.compile(about);
			if (partner!=null) this.partner = Pattern.compile(partner);
		}
		@Override
		public boolean edit(RDFEvent event) {
			if (!event.isTriplePattern()) return false;
			TriplePattern t = event.asTriplePattern();
			return !(match(about,t.getAbout()) && match(partner,t.getPartner()));
		}
	}
	
	public class PhoneMatchInsert implements Editor {
		Pattern subjectPattern;
		String keyword;
		boolean includesNamespace = false;
		public PhoneMatchInsert(String subjectRegex, String keyword) {
			super();
			this.subjectPattern = Pattern.compile(subjectRegex);
			this.keyword = keyword;
		}
		@Override
		public boolean edit(RDFEvent event) {
			if (!includesNamespace && event.isNamespace()) {
				if (event.asNamespace().getPrefix().equals("keyword")) {
					includesNamespace = true;
				}
			} else if (!includesNamespace && !event.isStartDocument() && !event.isComment()) {
				add(new Namespace("keyword", KEYWORD_NS));
				includesNamespace = true;
			}
			if (!event.isEndSubject()) return false;
			VarOrTerm vt = event.asSubject().getSubject();
			if (vt.isVar() 
			&& !vt.asVar().stringValue().startsWith("_") 
			&& match(subjectPattern,vt)) {
				Var var = tf.var(vt.stringValue() + "_phone");
				add(new TriplePattern(vt,tf.curie(KEYWORD_NS, "phone", "keyword"),var));
				// eg. FILTER sameTerm(?vt,keyword:soundex(keyword))
				add(new Filter(OPEN));
				add(new BuiltInCall(OPEN, "sameTerm"));
				add(new VarOrTermExpression(var));
				add(new BuiltInCall(OPEN, "keyword:soundex"));
				add(new VarOrTermExpression(tf.literal(keyword)));
				add(new BuiltInCall(CLOSE, "keyword:soundex"));
				add(new BuiltInCall(CLOSE, "sameTerm"));
				add(new Filter(CLOSE));
			}
			return false;
		}
	}
	
	public class FilterKeywordExists implements Editor {
		Pattern subjectPattern;
		VarOrTerm subject, object;
		String keyword;
		public FilterKeywordExists(String subjectRegex, String keyword) {
			super();
			this.subjectPattern = Pattern.compile(subjectRegex);
			this.keyword = keyword;
		}
		@Override
		public boolean edit(RDFEvent event) {
			// the subject of the filter is the first matching subject
			if (event.isTriplePattern()) {
				TriplePattern t = event.asTriplePattern();
				if (subject==null && match(subjectPattern,t.getSubject())) {
					subject = t.getAbout();
					object = tf.var("__label");
				}
			}
			// add the filter exists at the end, use "__" prefix for introduced variables
			// these variables are extraneous to the template (having no origin)
			else if (event.isEndWhere() && subject!=null) {
				add(new Filter(OPEN));
				add(new Exists(OPEN));
				
				VarOrTerm subj = subject;
				Var pred = tf.var("__label_property");
				add(new TriplePattern(subj,pred,object));
				// eg. FILTER regex(?object,keyword:regex($keyword))
				add(new Filter(OPEN));
				add(new BuiltInCall(OPEN, "regex"));
				add(new VarOrTermExpression(object));
				add(new BuiltInCall(OPEN, "keyword:regex"));
				add(new VarOrTermExpression(tf.literal(keyword)));
				add(new BuiltInCall(CLOSE, "keyword:regex"));
				add(new BuiltInCall(CLOSE, "regex"));
				add(new Filter(CLOSE));

				add(new Exists(CLOSE));
				add(new Filter(CLOSE));
			}
			return false;
		}		
	}
	
	public class TriplePatternRecorder implements Editor {
		URI pred;
		Pattern subjectPattern, objectPattern;
		List<TriplePattern> triples = new LinkedList<TriplePattern>();
		public TriplePatternRecorder(String subjectRegex, URI pred, String objectRegex) {
			if (subjectRegex!=null) subjectPattern = Pattern.compile(subjectRegex);
			if (objectRegex!=null) objectPattern = Pattern.compile(objectRegex);
			this.pred = pred;
		}
		@Override
		public boolean edit(RDFEvent event) {
			if (!event.isTriplePattern()) return false;
			TriplePattern t = event.asTriplePattern();
			if (match(subjectPattern, t.getSubject())
			&& (pred==null || t.getPredicate().stringValue().equals(pred.stringValue()))
			&& match(objectPattern, t.getObject()))
				triples.add(t);
			return false;
		}
		public List<TriplePattern> getTriplePatterns() {
			return triples;
		}
	}
		
	private boolean match(Pattern p, VarOrTerm vt) {
		if (p!=null && vt!=null && vt.isVar()) {
			Var v = vt.asVar();
			String origin = origins.get(v.stringValue());
			return p.matcher(origin).matches();
		}
		return true;
	}	

	public SPARQLPosteditor(RDFEventReader reader, Map<String, String> origins) {
		super(reader);
		this.origins = origins;
	}
	
	public SPARQLPosteditor(SPARQLProducer producer) {
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
