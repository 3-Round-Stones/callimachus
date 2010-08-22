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

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Optional;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.Node;
import org.callimachusproject.rdfa.model.Term;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrTerm;

/**
 * Reads RDF triples and converts them into OPTIONAL basic graph patterns.
 * 
 * @author James Leigh
 */
public class GraphPatternReader extends PipedRDFEventReader {
	private static final Pattern VAR_REGEX = Pattern
			.compile("[a-zA-Z0-9_"
					+ "\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD"
					+ "\\u00B7\\u0300-\\u036F\\u203F-\\u2040]+");
	private static final Pattern WHITE_SPACE = Pattern.compile("\\s*");
	private Deque<Integer> endOptional = new LinkedList<Integer>();
	private ArrayList<VarOrTerm> branch = new ArrayList<VarOrTerm>();
	private int depth = 0;
	private int detached = Integer.MAX_VALUE;
	private AtomicInteger seq = new AtomicInteger(0);
	private TermFactory tf = TermFactory.newInstance();

	public GraphPatternReader(RDFEventReader reader) {
		super(reader);
	}

	protected void process(RDFEvent event) throws RDFParseException {
		if (event.isStartDocument() || event.isEndDocument()) {
			add(event);
		} else if (event.isBase() || event.isNamespace()) {
			add(event);
		} else if (event.isStartSubject()) {
			VarOrTerm subj = event.asSubject().getSubject();
			subj = getVarOrTerm(subj);
			if (subj.isVar() && (endOptional.isEmpty() || depth != endOptional.peekLast())) {
				add(new Optional(true));
				endOptional.add(depth);
			}
			depth++;
			add(new Subject(true, subj));
		} else if (event.isTriple()) {
			Triple triple = event.asTriple();
			Node subj = triple.getSubject();
			IRI pred = triple.getPredicate();
			Term obj = triple.getObject();
			VarOrTerm s = getVarOrTerm(subj);
			VarOrTerm o = getVarOrTerm(obj);
			RDFEvent peek = peekNext();
			boolean optional = o.isVar(); 
			boolean newSubject = peek.isStartSubject();
			boolean nested = newSubject && obj.equals(peek.asSubject().getSubject());
			if (s.isVar() && depth < detached) {
				boolean attached = false;
				for (VarOrTerm parent : branch) {
					if (subj.equals(parent)) {
						attached = true;
					}
				}
				if (!attached) {
					detached = depth;
				}
			}
			if (optional) {
				add(new Optional(true));
			}
			add(new TriplePattern(s, pred, o, triple.isInverse()));
			if (nested) {
				branch.ensureCapacity(depth + 1);
				while (branch.size() <= depth + 1) {
					branch.add(null);
				}
				branch.set(depth + 1, peek.asSubject().getSubject());
			}
			if (optional && nested) {
				endOptional.add(depth);
			} else if (optional) {
				add(new Optional(false));
			}
		} else if (event.isEndSubject()) {
			VarOrTerm subj = event.asSubject().getSubject();
			add(new Subject(false, getVarOrTerm(subj)));
			if (depth < branch.size()) {
				branch.set(depth, null);
			}
			depth--;
			if (!endOptional.isEmpty() && depth == endOptional.peekLast()) {
				endOptional.removeLast();
				add(new Optional(false));
			}
			if (detached == depth) {
				detached = Integer.MAX_VALUE;
			}
		}
	}

	private VarOrTerm getVarOrTerm(VarOrTerm term) throws RDFParseException {
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

	private boolean isEmpty(String str) {
		return "".equals(str) || WHITE_SPACE.matcher(str).matches();
	}
}
