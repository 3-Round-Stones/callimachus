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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.Reference;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrIRI;
import org.callimachusproject.rdfa.model.VarOrTerm;

/**
 * Projects triple patterns using the variable as the predicate.
 * 
 * @author James Leigh
 *
 */
public class TriplePatternVariableStore extends TriplePatternStore {
	private static final String v = "http://callimachusproject.org/rdf/2009/framework/variables/?";
	private TermFactory tf = TermFactory.newInstance();

	public TriplePatternVariableStore(String base) {
		super(base);
	}

	public TriplePattern getProjectedPattern(TriplePattern tp) {
		VarOrTerm subj = tp.getSubject();
		VarOrIRI pred = tp.getPredicate();
		VarOrTerm obj = tp.getObject();
		VarOrTerm ptr = tp.getPartner();
		if (ptr.isVar() && !(ptr instanceof BlankOrLiteralVar)) {
			pred = tf.iri(v + ptr.stringValue());
		}
		return new TriplePattern(subj, pred, obj, tp.isInverse());
	}

	protected List<TriplePattern> getConstructPatterns(
			List<RDFEvent> where) {
		Set<VarOrTerm> variables = new HashSet<VarOrTerm>();
		List<TriplePattern> list = new ArrayList<TriplePattern>(where.size());
		for (RDFEvent event : where) {
			if (event.isTriplePattern()) {
				TriplePattern tp = event.asTriplePattern();
				VarOrTerm subj = tp.getSubject();
				VarOrTerm obj = tp.getObject();
				if (subj.isVar() && !(subj instanceof BlankOrLiteralVar) && !variables.contains(subj)) {
					VarOrIRI pred = tf.iri(v + subj.stringValue());
					if (tp.isInverse()) {
						list.add(new TriplePattern(obj, pred, subj));
					} else {
						Reference ref = tf.reference(resolve(getReference()), getReference());
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
		return list;
	}

}
