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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Ask;
import org.callimachusproject.rdfa.events.Group;
import org.callimachusproject.rdfa.events.Optional;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Select;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.events.Union;
import org.callimachusproject.rdfa.events.Where;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrIRI;
import org.callimachusproject.rdfa.model.VarOrTerm;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * Produce SPARQL events from an RDFa event stream. 
 * 
 * @author James Leigh
 * @author Steve Battle
 */
public class SPARQLProducer extends BufferedRDFEventReader {
	private static final Pattern VAR_REGEX = Pattern
		.compile("[a-zA-Z0-9_"
		+ "\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD"
		+ "\\u00B7\\u0300-\\u036F\\u203F-\\u2040]+");
	private static final Pattern WHITE_SPACE = Pattern.compile("\\s*");
		
	private static boolean OPEN = true, CLOSE = false;
	private Stack<Context> stack = new Stack<Context>();	
	private TermFactory tf = TermFactory.newInstance();
		
	// map unique resource IDs to variable labels
	// resources can be objects or literals
	private Map<String,String> varMap = new HashMap<String,String>();

	// map variable labels to a label counter
	private Map<String,AtomicInteger> varSeq = new HashMap<String,AtomicInteger>();
	
	// map variable names to template origin (path)
	private Map<String,String> origins = new HashMap<String,String>();
		
	// 'initial' controls placement of the 'UNION' keyword
	private boolean initial = true;
	
	// keep a record of the most recent triple pattern
	TriplePattern previousPattern = null;
	
	static enum CLAUSE { GROUP, OPTIONAL, BLOCK }
	
	public static enum QUERY { SELECT, CONSTRUCT, ASK }

	// construct queries are the default
	private QUERY queryType=QUERY.CONSTRUCT;
	
	
	public boolean isSelectQuery() {
		return queryType==QUERY.SELECT;
	}
	
	public boolean isAskQuery() {
		return queryType==QUERY.ASK;
	}

	// determine if a new subject is chained to the previous triple
	static boolean chained(TriplePattern pattern, VarOrTerm subject) {
		return pattern!=null && subject!=null && 
		(subject.equals(pattern.getPartner()) 
		|| (subject.equals(pattern.getAbout()) && !pattern.getPartner().isVar())
		);
	}
	
	class Context {
		CLAUSE clause = CLAUSE.BLOCK;
		// a block may have a subject (the outermost block does not)
		VarOrTerm subject;
		// 'union' is set when a context is opened
		// for triple-blocks 'union' is inherited from parent
		boolean union = false;
		
		/* create a new subject block */
		Context(VarOrTerm subject) throws RDFParseException {
			this.subject = subject;
			// triple blocks inherit union status from parent
			union = stack.isEmpty()?false:stack.peek().union;
		}
		
		/* create a new clause */
		Context(CLAUSE clause) {
			this.clause = clause;
			// triple blocks inherit union status from parent
			union = stack.isEmpty()?false:stack.peek().union;
		}
		
		public String toString() {
			StringBuffer b = new StringBuffer();
			b.append("(");
			b.append(clause);
			b.append(","+subject);
			if (union) b.append(",union");
			b.append(")");
			return b.toString();
		}
		boolean hasSubject() {
			return subject!=null;
		}
		boolean isOptional() {
			return clause==CLAUSE.OPTIONAL;
		}
		boolean isGroup() {
			return clause==CLAUSE.GROUP;
		}
		boolean isBlock() {
			return clause==CLAUSE.BLOCK;
		}
		
		/* push the context adding necessary open brackets */
		Context open() {
			if (isOptional()) {
				add(new Optional(OPEN));
				// the first clause of the optional is the LHS of a UNION
				union = true;
			}
			else if (isGroup()) {
				// the parent group may be a union
				if (!stack.isEmpty() && stack.peek().union && !initial) 
					add(new Union());
				add(new Group(OPEN));
				// a group (i.e. a UNION sub-clause) represents a conjunction / join
				union = false;
			}
			if (hasSubject()) add(new Subject(OPEN, subject));
			// triple blocks do not represent the start of a group
			if (!isBlock()) initial = true;
			return stack.push(this);
		}
		
		/* pop the context adding necessary close brackets */
		Context close() {
			if (hasSubject()) add(new Subject(CLOSE, subject));
			if (isOptional()) add(new Optional(CLOSE));
			else if (isGroup()) add(new Group(CLOSE));
			if (!isBlock()) previousPattern = null;
			stack.pop();
			// closing the LHS of a UNION? (UNION keyword required subsequently)
			if (!stack.isEmpty() && stack.peek().union) initial = false;
			return stack.isEmpty()?null:stack.peek();
		}
	}

	public SPARQLProducer(RDFEventReader reader) {
		super(reader);
	}
	
	public SPARQLProducer(RDFEventReader reader, QUERY queryType) {
		super(reader);
		this.queryType = queryType;
	}
	
	protected void process(RDFEvent event) throws RDFParseException {
		if (event==null) return;
		if (event.isStartDocument()) {
			add(event);
			// add the first occurrence of the base
			addBase();
			// add all namespace definitions
			addPrefixes();
			// don't open a where clause for construct queries (here)
			if (isSelectQuery()) {
				add(new Select());
				add(new Where(OPEN));
			}
			else if (isAskQuery()) {
				add(new Ask());
				add(new Where(OPEN));
			}
		}
		else if (event.isEndDocument()) {
			// close any remaining open contexts (i.e. the outermost context)
			while (!stack.isEmpty()) stack.peek().close();
			if (isSelectQuery() || isAskQuery()) add(new Where(CLOSE));
			add(event);
		}
		else if (event.isStartSubject()) {
			VarOrTerm subj = getVarOrTerm(event.asSubject().getSubject(),null);
			Context context = getContext();

			// close any dangling UNION sub-clause if the subject isn't chained to the previous triple
			if (context.isGroup() && !context.hasSubject() && !chained(previousPattern,subj)) 
				context = context.close();

			// create a (unbracketed triple block) context for the new subject
			context = new Context(subj).open();	
			
			// assemble mandatory conditions at the beginning of the block
			// if not in a union mandatory triples are inner-joined
			context = addMandatoryTriples(context);
			
			// subjects chained to the previous triple can be left-joined
			// this may be a singleton if no optional properties are defined
			// in which case the optional is not required
			if (!context.union && chained(previousPattern,subj) && !singleton(context))
				context = new Context(CLAUSE.OPTIONAL).open();
			
			previousPattern = null;
		} 
		else if (event.isTriple()) {
			Triple triple = event.asTriple();
			IRI p = triple.getPredicate();
			boolean rev = triple.isInverse();
			VarOrTerm s = getVarOrTerm(triple.getSubject(),triple);
			VarOrTerm o = getVarOrTerm(triple.getObject(),triple);

			boolean optional = isOptionalTriple(triple); 	
			TriplePattern pattern = new TriplePattern(s, p, o, rev);
			//String lang = triple.getPartner().asPlainLiteral().getLang();
			Context context = getContext();
						
			// close any dangling UNION sub-clause
			// i.e don't close if this is a union
			// and don't close if this is the first (non mandatory) triple
			if (!context.union && !initial && context.isGroup() && !context.hasSubject()) 
				context = context.close();
			
			if (optional) {
				// an optional triple must be within an optional/union clause
				if (!context.union) 
					context = new Context(CLAUSE.OPTIONAL).open();
				
				// open a group (a UNION sub-clause) unless this is a singleton in an OPTIONAL
				if (!(initial && singleton(context)) || !context.isOptional())
					context = new Context(CLAUSE.GROUP).open();
				
				// add the triple pattern
				add(pattern);
				
				// subsequent triples/subjects are not in the initial position
				initial = false;
				previousPattern = pattern;
			}
		} 
		else if (event.isEndSubject()) {
			Context context = stack.peek();
			
			// close dangling OPTIONALs or UNIONs
			while (!context.hasSubject()) context = context.close();
			
			// then close the subject context
			context = context.close();
		}
	}

	private void addBase() throws RDFParseException {
		int lookAhead=0;
		RDFEvent e;
		do {
			e = peek(lookAhead++);
			if (e.isBase()) {
				add(e);
				break;
			}
		} while (!e.isEndDocument());
	}

	private void addPrefixes() throws RDFParseException {
		int lookAhead=0;
		RDFEvent e;
		do {
			e = peek(lookAhead++);
			if (e.isNamespace())
				add(e);
		} while (!e.isEndDocument());		
	}

	private Context getContext() {
		Context context = null;
		// the outermost context is a triple block (with no subject)
		if (stack.isEmpty()) {
			context = new Context(CLAUSE.BLOCK).open();
			// The outer block a UNION
			context.union = true;
		}
		else context = stack.peek();
		return context;
	}
	
	private boolean isOptionalTriple(Triple triple) throws RDFParseException {
		// This may be an object variable
		return getVarOrTerm(triple.getPartner(),null).isVar();
	}

	private Context addMandatoryTriples(Context context) throws RDFParseException {
		int lookAhead=0, depth=0;
		RDFEvent e;
		while (depth>=0) { // look-ahead until we reach corresponding close subject
			e = peek(lookAhead++);
			if (e.isStartSubject()) depth++;
			else if (e.isEndSubject()) depth-- ;
			// don't consider mandatory triples in nested contexts
			else if (e.isTriple() && depth==0) {
				Triple triple = e.asTriple();				
				if (!isOptionalTriple(triple)) {
					IRI p = triple.getPredicate();
					boolean rev = triple.isInverse();
					VarOrTerm s = getVarOrTerm(triple.getSubject(),triple);
					VarOrTerm o = getVarOrTerm(triple.getObject(),triple);
					
					if (context.union)
						context = new Context(CLAUSE.GROUP).open();
					
					TriplePattern pattern = new TriplePattern(s, p, o, rev);			
					add(pattern);
					previousPattern = pattern;
					// don't reset 'initial' otherwise the next optional triple will close the group
				}
			}
		}
		return context;
	}
	
	private boolean singleton(Context context) throws RDFParseException {
		int lookAhead=0, depth=0;
		boolean singleton=true;
		RDFEvent e;
		while (depth>=0 && singleton) {
			e = peek(lookAhead++);
			if (e==null) break;
			if (e.isStartSubject()) {
				singleton = false;				
				depth++;
			}
			else if (e.isEndSubject()) depth--;
			else if (e.isTriple() && depth==0) {
				Triple t = e.asTriple();
				// ignore mandatory triples
				singleton = !isOptionalTriple(t);
			}
		}
		return singleton;
	}
	
	String stripPrefix(String l, String prefix) {
		int p = prefix.length();
		if (l.startsWith(prefix) && l.length()>p
		&& l.substring(p,p+1).equals(l.substring(p,p+1).toUpperCase())) {
			l = l.substring(p,p+1).toLowerCase() + l.substring(p+1);
		}
		return l;
	}
	
	String stripSuffix(String l, String suffix) {
		int s = suffix.length();
		if (l.endsWith(suffix) && l.length()>s) {
			l = l.substring(0,l.length()-s);
		}
		return l;
	}
	
	String predLabel(IRI pred) {
		URI uri=null;
		if (pred.isCURIE())
			uri = new URIImpl(pred.asCURIE().stringValue());
		else if (pred.isIRI())
			uri = new URIImpl(pred.stringValue());
		String l = uri.getLocalName();
		l = stripPrefix(l,"has");
		l = stripPrefix(l,"in");
		return l;
	}
	
	String mapSeq(String label, boolean increment) {
		if (label==null) return "";
		if (varSeq.containsKey(label)) {
			if (increment)
				return label + varSeq.get(label).incrementAndGet();
			else {
				int n = varSeq.get(label).intValue();
				return label + (n==0?"":n);
			}
		}
		else {
			varSeq.put(label, new AtomicInteger(0));
			// implicit '0' on label
			return label;
		}
	}
	
	// the key is a unique string identifying the resource
	
	String mapVar(String key, String label) {
		if (varMap.containsKey(key)) {
			label = varMap.get(key);
			return mapSeq(label,false);
		}
		else if (label==null) return "";
		else {
			varMap.put(key, label);
			return mapSeq(label,true);
		}
	}
	
	protected VarOrTerm getVarOrTerm(VarOrTerm term, Triple triple) throws RDFParseException {
		String label = null;
		VarOrTerm opposite = null;
		if (triple!=null) {
			label = predLabel(triple.getPredicate());
			// characters valid in NCName but not SPARQL variable name
			label = label.replaceAll("-", "_").replaceAll("\\.", "_");
			
			// append/remove 'Of' to inverse relations
			if (triple.isInverse()) {
				if (label.endsWith("Of")) label = stripSuffix(label,"Of");
				else label += "Of";
			}

			// build a compound label using variable at the opposing end of the triple
			if (term.equals(triple.getSubject())) 
				opposite = triple.getObject();
			else opposite = triple.getSubject();
			if (!opposite.isIRI()) {
				label = mapVar(opposite.stringValue(),null) + "_" + label;
			}
			else if (opposite.isReference()){
				String v = opposite.asReference().getRelative();
				if (v.startsWith("?") && !v.equals("?this")) 
					label = v.substring(1) + "_" + label;
			}
		}
		
		if (term.isVar())
			return term;
		if (term.isLiteral() && isEmpty(term.stringValue())) {
			String name = "_"+mapSeq(label,true);
			if (label!=null) origins.put(name,term.getOrigin());
			return new BlankOrLiteralVar(name);
		}
		// a non-empty literal may represent a literal variable
		if (term.isLiteral()) {
			if (term.stringValue().startsWith("?")) {
				String name = term.stringValue().substring(1);
				VarOrTerm v = tf.var(name);
				v.setOrigin(term.getOrigin());
				origins.put(name,term.getOrigin());
				return v;
			}
			else return term;
		}
		
		if (!term.isIRI()) {
			String name = "_" + mapVar(term.stringValue(),label);
			if (label!=null) origins.put(name,term.getOrigin());
			return new BlankOrLiteralVar(name);
		}
		if (term.isCURIE())
			return term;
		if (!term.isReference())
			return term;
		String var = term.asReference().getRelative();
		if (!var.startsWith("?")) return term;
		String name = var.substring(1);
		if (!VAR_REGEX.matcher(name).matches())
			throw new RDFParseException("Invalid Variable Name: " + name);
		if (label!=null) origins.put(name,term.getOrigin());
		return tf.var(name);
	}

	protected boolean isEmpty(String str) {
		return "".equals(str) || WHITE_SPACE.matcher(str).matches();
	}
		
	public Map<String,String> getOrigins() {
		return origins;
	}
}
