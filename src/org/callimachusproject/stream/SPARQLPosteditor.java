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

package org.callimachusproject.stream;

/**
 * Edit an XHTML+RDFa event stream, conditionally add/remove events 
 * 
 * @author Steve Battle
 */

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.BuiltInCall;
import org.callimachusproject.rdfa.events.ConditionalOrExpression;
import org.callimachusproject.rdfa.events.Exists;
import org.callimachusproject.rdfa.events.Expression;
import org.callimachusproject.rdfa.events.Filter;
import org.callimachusproject.rdfa.events.Group;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.events.Union;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.Var;
import org.callimachusproject.rdfa.model.VarOrTerm;
import org.openrdf.model.URI;

public class SPARQLPosteditor extends BufferedRDFEventReader {
	private static boolean OPEN = true, CLOSE = false;

	private static TermFactory tf = TermFactory.newInstance();

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
	
	public class ConditionInsert implements Editor {
		IRI pred;
		PlainLiteral lit;
		Pattern subjectPattern;
		public ConditionInsert(String subjectRegex, IRI pred, PlainLiteral lit) {
			super();
			this.subjectPattern = Pattern.compile(subjectRegex);
			this.pred = pred;
			this.lit = lit;
		}
		@Override
		public boolean edit(RDFEvent event) {
			if (!event.isEndSubject()) return false;
			VarOrTerm vt = event.asSubject().getSubject();
			if (vt.isVar() 
			&& !vt.asVar().stringValue().startsWith("_") 
			&& match(subjectPattern,vt)) {
				add(new TriplePattern(vt,pred,lit));
			}
			return false;
		}
	}

	public class FilterInsert implements Editor {
		List<String> props;
		Pattern subjectPattern;
		String regex;
		List<RDFEvent> list = new LinkedList<RDFEvent>();
		public FilterInsert(String subjectRegex, String[] props, String regex) {
			super();
			this.props = Arrays.asList(props);
			this.subjectPattern = Pattern.compile(subjectRegex);
			this.regex = regex;
		}
		@Override
		public boolean edit(RDFEvent event) {
			if (event.isTriplePattern()) {
				TriplePattern t = event.asTriplePattern();
				if (match(subjectPattern,t.getSubject())) {
					if (props.contains(t.getPredicate().stringValue())) {
						if (list.size()>0) list.add(new ConditionalOrExpression());
						// eg. FILTER regex( str(<object>),<regex>,i)
						list.add(new BuiltInCall(OPEN, "regex"));
						list.add(new BuiltInCall(OPEN, "str"));
						list.add(new Expression(t.getObject()));
						list.add(new BuiltInCall(CLOSE, "str"));
						list.add(new Expression(tf.literal(regex)));
						list.add(new Expression(tf.literal("i")));
						list.add(new BuiltInCall(CLOSE, "regex"));
					}
				}
			}
			else if (event.isEndWhere()) {
				addAll(list);
			}
			return false;
		}
	}
	
	public class FilterExists implements Editor {
		List<String> props;
		Pattern subjectPattern;
		VarOrTerm subject, object;
		String regex;
		public FilterExists(String subjectRegex, String[] props, String regex) {
			super();
			this.subjectPattern = Pattern.compile(subjectRegex);
			this.props = Arrays.asList(props);
			this.regex = regex;
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
				
				boolean first = true;
				for (String prop: props) {
					if (!first) add(new Union());
					add(new Group(OPEN));
					VarOrTerm subj = subject;
					IRI pred = tf.iri(prop);
					add(new TriplePattern(subj,pred,object));
					add(new Group(CLOSE));
					first = false;
				}
				// eg. FILTER regex( str(<object>),<regex>,i)
				add(new Filter(OPEN));
				add(new BuiltInCall(true, "regex"));
				add(new BuiltInCall(true, "str"));
				add(new Expression(object));
				add(new BuiltInCall(false, "str"));
				add(new Expression(tf.literal(regex)));
				add(new Expression(tf.literal("i")));
				add(new BuiltInCall(false, "regex"));
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
