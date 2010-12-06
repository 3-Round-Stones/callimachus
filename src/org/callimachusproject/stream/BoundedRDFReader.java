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

import java.util.LinkedList;
import java.util.Queue;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.model.VarOrIRI;
import org.callimachusproject.rdfa.model.VarOrTerm;

/**
 * Filters out any triples that are about a nested named resource.
 * 
 * @author James Leigh
 *
 */
public class BoundedRDFReader extends PipedRDFEventReader {
	private static String FIRST = "http://www.w3.org/1999/02/22-rdf-syntax-ns#first";
	private LinkedList<Queue<RDFEvent>> stack = new LinkedList<Queue<RDFEvent>>();
	private int bound;

	public BoundedRDFReader(RDFEventReader reader) {
		super(reader);
	}

	@Override
	protected void process(RDFEvent next) throws RDFParseException {
		if (next.isStartSubject()) {
			VarOrTerm subj = next.asSubject().getSubject();
			if (extarnalVar(subj)) {
				stack.addLast(new LinkedList<RDFEvent>());
			}
		}
		if (next.isTriplePattern()) {
			// rdf:List can be connected by any variable
			VarOrIRI pred = next.asTriplePattern().getPredicate();
			if (pred.stringValue().equals(FIRST)) {
				for (Queue<RDFEvent> queue : stack) {
					while (!queue.isEmpty()) {
						add(queue.remove());
					}
				}
				if (bound < stack.size()) {
					bound = stack.size();
				}
			}
		}
		if (stack.size() <= bound) {
			add(next);
		}
		if (next.isEndSubject()) {
			VarOrTerm subj = next.asSubject().getSubject();
			if (extarnalVar(subj)) {
				stack.removeLast();
				if (bound > stack.size()) {
					bound = stack.size();
				}
			}
		}
	}

	private boolean extarnalVar(VarOrTerm subj) {
		if (subj instanceof BlankOrLiteralVar)
			return false;
		if (subj.isVar() && !"this".equals(subj.stringValue()))
			return true;
		return false;
	}
}
