/*
 * Copyright (c) 2010, Zepheira LLC and James Leigh Some rights reserved.
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
package org.callimachusproject.behaviours;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.concepts.Template;
import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Graph;
import org.callimachusproject.rdfa.events.Group;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.events.Union;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.Var;
import org.callimachusproject.rdfa.model.VarOrTerm;
import org.callimachusproject.stream.PipedRDFEventReader;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.sail.auditing.vocabulary.Audit;

/**
 * Overrides audit:revision patterns to include all historic possible values as
 * well.
 * 
 * @author James Leigh
 * 
 */
public abstract class ReviewSupport extends ViewSupport implements Template,
		RDFObject {
	private static String VARPREFIX = "review"
			+ Long.toHexString(System.nanoTime()) + "x";

	private static final String REVISION = Audit.REVISION.stringValue();
	private static final String CONTAINED = Audit.CONTAINED.stringValue();
	private static final String SUBJECT = RDF.SUBJECT.stringValue();

	@Override
	protected RDFEventReader readPatterns(String mode, String el,
			String about) throws XMLStreamException, IOException,
			TransformerException {
		return new PipedRDFEventReader(super.readPatterns(mode, el, about)) {
			TermFactory tf = TermFactory.newInstance();
			IRI revision = tf.iri(REVISION);
			IRI contained = tf.iri(CONTAINED);
			IRI subject = tf.iri(SUBJECT);
			int c;

			protected void process(RDFEvent next) throws RDFParseException {
				if (isRevisionPattern(next)) {
					TriplePattern tp = next.asTriplePattern();
					VarOrTerm s = tp.getSubject();
					Var var = tp.getObject().asVar();
					add(new Group(true));
					add(tp);
					add(new Group(false));
					add(new Union());
					add(new Group(true));
					add(new Graph(true, var));
					add(new TriplePattern(s, nextVar(), nextVar()));
					add(new Graph(false, var));
					add(new Group(false));
					add(new Union());
					add(new Group(true));
					Var triple = nextVar();
					add(new TriplePattern(var, contained, triple));
					add(new TriplePattern(triple, subject, s));
					add(new Group(false));
				} else {
					add(next);
				}
			}

			private Var nextVar() {
				return tf.var(VARPREFIX + c++);
			}

			private boolean isRevisionPattern(RDFEvent next) {
				if (!next.isTriplePattern())
					return false;
				TriplePattern tp = next.asTriplePattern();
				if (!revision.equals(tp.getPredicate()))
					return false;
				return tp.getObject().isVar();
			}
		};
	}

}
