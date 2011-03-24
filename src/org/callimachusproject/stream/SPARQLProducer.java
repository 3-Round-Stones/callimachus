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
	
	// determine if a new subject is well-joined in this context
	static boolean wellJoined(TriplePattern pattern, VarOrTerm subject) {
		return pattern!=null && subject!=null &&
		(subject.equals(pattern.getSubject()) || subject.equals(pattern.getObject()) );
	}
	
	class Context {
		CLAUSE clause = CLAUSE.BLOCK;
		// a block may have a subject (the outermost block does not)
		VarOrTerm subject;
		// 'union' is set once we have begun a UNION in this context
		boolean union = false;
		int mandatoryCount=0, optionalCount=0, subjectCount=0;
		
		/* create a new subject block */
		Context(VarOrTerm subject) throws RDFParseException {
			this.subject = subject;
			// a nesting block may have begun a UNION
			union = stack.peek().union;
			return;
		}
		/* create a new clause */
		Context(CLAUSE clause) {
			this.clause = clause;
			// a block (unbracketed) inherits union from its parent
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
			stack.pop();
			// a nested block may have begun a UNION
			if (!stack.isEmpty() && stack.peek().isBlock()) {
				stack.peek().union |= union && !stack.peek().isOptional();
			}
			return stack.isEmpty() ? null : stack.peek();
		}
	}

	public SPARQLProducer(RDFEventReader reader) {
		super(reader);
	}
	
	protected void process(RDFEvent event) throws RDFParseException {

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
			Context context;
			
			// the outermost context is a triple block (with no subject)
			if (stack.isEmpty()) {
				context = new Context(CLAUSE.BLOCK).open();
				// make the outer block a UNION
				context.union = true;
			}
			else context = stack.peek();

			// close any dangling UNION sub-clause if the subject isn't well-joined
			if (!initial && context.isGroup() && !context.hasSubject() && !wellJoined(previousPattern,subj)) 
				context = context.close();

			// create a (triple block) context for the new subject (no brackets)
			context = new Context(subj).open();
			
			// Don't add a subject directly to an optional - enclose within a group
			if (context.union) {
				if (!initial) add(new Union()) ;
				context = new Context(CLAUSE.GROUP).open();
			}
						
			// assemble mandatory conditions at beginning of the block
			// initialize mandatoryCount and optionalCount
			addMandatoryTriples(context);
			
			// if there are optional or potentially optional triples, add an optional clause
			if (context.optionalCount>0 || context.subjectCount>0) {
				// the block is well-joined if it is chained to the previous triple pattern
				// also if it contains any mandatory triples (appended above) sharing the same subject
				boolean wellJoined =  context.mandatoryCount>0 || wellJoined(previousPattern,subj);
				
				// We need a left join with the empty group (a single empty solution)
				// { OPTIONAL { P }} equivalent to {} OPTIONAL { P }
				
				// only open the group if we didn't open it above
				if (!wellJoined && !context.isGroup())
					context = new Context(CLAUSE.GROUP).open();

				// in both cases we add the OPTIONAL
				context = new Context(CLAUSE.OPTIONAL).open();

				previousPattern = null;
			}
		} 
		else if (event.isTriple()) {
			Triple triple = event.asTriple();
			VarOrTerm s = getVarOrTerm(triple.getSubject());
			IRI p = triple.getPredicate();
			VarOrTerm o = getVarOrTerm(triple.getObject());
			boolean rev = triple.isInverse();
			boolean optional = getVarOrTerm(triple.getPartner()).isVar(); 	
			Context context = stack.peek();
			TriplePattern pattern = new TriplePattern(s, p, o, rev);
			
			// close any dangling UNION sub-clause
			if (!initial && context.isGroup() && !context.hasSubject()) context = context.close();
			
			// if this group is a singleton, then treat it like a conjunction (no inner brackets)
			context.union = !singleton(false);
			
			// an optional triple may open a new union context
			if (optional) {
				// add the 'UNION' keyword if this is not the first UNION sub-clause
				if (context.union && !initial) add(new Union()) ;

				// open a group for the UNION sub-clause
				// remove redundant brackets under the following conditions:
				// if this is a singleton optional (ignoring mandatory triples)
				// new subjects are contained in a joined optional so don't harm single status
				//if (!singleton(false,true))
				if (context.union)
					context = new Context(CLAUSE.GROUP).open();
				
				// add the triple pattern
				add(previousPattern = pattern);
				// subsequent triples/subjects are not in the initial position
				initial = false;
				
				// if this is followed by >1 subject add an optional
				if (initial && multipleSubjects())
					new Context(CLAUSE.OPTIONAL).open();
			}
			else { 
				// this is a mandatory triple (already added at start of subject)
				// used here only to determine if subsequent subjects are well-joined
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

	private void addMandatoryTriples(Context context) throws RDFParseException {
		int lookAhead=0, depth=0;
		RDFEvent e;
		while (depth>=0) { // look-ahead until we reach corresponding close subject
			e = peek(lookAhead++);
			if (e.isStartSubject()) {
				depth++;
				context.subjectCount++;
			}
			else if (e.isEndSubject()) depth-- ;
			// don't consider mandatory triples in nested contexts
			else if (e.isTriple() && depth==0) {
				Triple triple = e.asTriple();
				boolean optional = getVarOrTerm(triple.getPartner()).isVar();
				if (optional) context.optionalCount++;
				else {
					context.mandatoryCount++;
					VarOrTerm s = getVarOrTerm(triple.getSubject());
					IRI p = triple.getPredicate();
					VarOrTerm o = getVarOrTerm(triple.getObject());
					boolean rev = triple.isInverse();

					TriplePattern pattern = new TriplePattern(s, p, o, rev);
					add(pattern);
					initial = false;
				}
			}
		}
	}

	private boolean singleton(boolean skipFirstOptional) throws RDFParseException {
		int lookAhead=0, depth=0;
		boolean singleton=initial;
		RDFEvent e;
		while (depth>=0 && singleton) {
			e = peek(lookAhead++);
			if (e.isStartSubject()) {
				singleton = false;				
				depth++;
			}
			else if (e.isEndSubject()) depth--;
			else if (e.isTriple() && depth==0) {
				Triple t = e.asTriple();
				boolean opt = getVarOrTerm(t.getPartner()).isVar();
				if (skipFirstOptional) skipFirstOptional = false;
				else singleton = !opt;
			}
		}
		return singleton;
	}
	
	private boolean multipleSubjects() throws RDFParseException {
		int lookAhead=0, depth=0;
		int subjectCount=0;
		RDFEvent e;
		while (depth>=0) {
			e = peek(lookAhead++);
			if (e.isStartSubject()) {
				subjectCount++; depth++;
			}
			else if (e.isEndSubject()) depth--;

		}
		return subjectCount>1;
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
