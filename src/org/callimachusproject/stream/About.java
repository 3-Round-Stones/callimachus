/*
 * Copyright (c) 2010, Zepheira LLC Some rights reserved.
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

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.model.VarOrTerm;

/**
 * Ensures an RDF stream includes at least a subject of the term given.
 * 
 * @author James Leigh
 * 
 */
public class About extends PipedRDFEventReader {
	private VarOrTerm about;
	private boolean begining = true;

	public About(VarOrTerm about, RDFEventReader reader) {
		super(reader);
		this.about = about;
	}

	@Override
	protected void process(RDFEvent next) throws RDFParseException {
		if (begining && about != null) {
			if (next.isEndDocument()) {
				begining = false;
				add(new Subject(true, about));
				add(new Subject(false, about));
			} else if (next.isStartSubject()) {
				begining = false;
				if (!next.asSubject().getSubject().equals(about)) {
					add(new Subject(true, about));
					add(new Subject(false, about));
				}
			} else if (next.isTriplePattern()) {
				begining = false;
				if (!next.asTriplePattern().getSubject().equals(about)) {
					add(new Subject(true, about));
					add(new Subject(false, about));
				}
			}
		}
		add(next);
	}

}
