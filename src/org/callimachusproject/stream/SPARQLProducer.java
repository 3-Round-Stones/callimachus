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

import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Group;
import org.callimachusproject.rdfa.events.Optional;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.events.Union;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrTerm;

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
	private AtomicInteger seq = new AtomicInteger(0);
	private TermFactory tf = TermFactory.newInstance();
	
	// 'initial' controls placement of the 'UNION' keyword
	private boolean initial = true;
	
	// keep a record of the most recent triple pattern
	TriplePattern previousPattern = null;
	
	static enum CLAUSE { GROUP, OPTIONAL, BLOCK }
	
	// determine if a new subject is chained to the previous triple
	static boolean chained(TriplePattern pattern, VarOrTerm subject) {
		return pattern!=null && subject!=null && 
		(subject.equals(pattern.getPartner()) 
		|| (subject.equals(pattern.getAbout()) && !pattern.getPartner().isVar())
		);
		//(subject.equals(pattern.getSubject()) || subject.equals(pattern.getObject()) );
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
	
	protected void process(RDFEvent event) throws RDFParseException {
		if (event==null) return;
		if (event.isStartDocument() || event.isBase() || event.isNamespace()) {
			add(event);
		}
		else if (event.isEndDocument()) {
			// close any remaining open contexts (i.e. the outermost context)
			while (!stack.isEmpty()) stack.peek().close();
			add(event);
		}
		else if (event.isStartSubject()) {
			VarOrTerm subj = getVarOrTerm(event.asSubject().getSubject());
			Context context = getContext();

			// close any dangling UNION sub-clause if the subject isn't chained to the previous triple
			if (context.isGroup() && !context.hasSubject() && !chained(previousPattern,subj)) 
				context = context.close();

			// assemble mandatory conditions at the beginning of the block
			// if not in a union mandatory triples are inner-joined
			context = addMandatoryTriples(context);
			
			// create a (unbracketed triple block) context for the new subject
			context = new Context(subj).open();	
			
			// subjects chained to the previous triple can be left-joined
			// this may be a singleton if no optional properties are defined
			// in which case the optional is not required
			if (!context.union && chained(previousPattern,subj) && !singleton())
				context = new Context(CLAUSE.OPTIONAL).open();
			
			previousPattern = null;
		} 
		else if (event.isTriple()) {
			Triple triple = event.asTriple();
			VarOrTerm s = getVarOrTerm(triple.getSubject());
			IRI p = triple.getPredicate();
			VarOrTerm o = getVarOrTerm(triple.getObject());
			boolean rev = triple.isInverse();
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
				if (!(initial && singleton()) || !context.isOptional())
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
		return getVarOrTerm(triple.getPartner()).isVar() ;
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
					VarOrTerm s = getVarOrTerm(triple.getSubject());
					IRI p = triple.getPredicate();
					VarOrTerm o = getVarOrTerm(triple.getObject());
					boolean rev = triple.isInverse();
					
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
	
	private boolean singleton() throws RDFParseException {
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
				// ignore mandatory triples
				singleton = !isOptionalTriple(e.asTriple());
			}
		}
		return singleton;
	}

	protected VarOrTerm getVarOrTerm(VarOrTerm term) throws RDFParseException {
		if (term.isVar())
			return term;
		if (term.isLiteral() && isEmpty(term.stringValue()))
			return new BlankOrLiteralVar("blank_" + seq.incrementAndGet());
		if (term.isLiteral())
			return term;
		if (!term.isIRI())
			return new BlankOrLiteralVar("blank_" + term.stringValue());
		if (term.isCURIE())
			return term;
		if (!term.isReference())
			return term;
		String var = term.asReference().getRelative();
		if (!var.startsWith("?"))
			return term;
		String name = var.substring(1);
		if (!VAR_REGEX.matcher(name).matches())
			throw new RDFParseException("Invalid Variable Name: " + name);
		return tf.var(name);
	}

	protected boolean isEmpty(String str) {
		return "".equals(str) || WHITE_SPACE.matcher(str).matches();
	}
}
