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

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Namespace;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.model.CURIE;

/**
 * Add an additional triple to the start of this RDFEventReader.
 * 
 * @author James Leigh
 *
 */
public class PrependTriple extends PipedRDFEventReader {
	private Triple triple;
	private boolean begining = true;
	private Map<String, String> namespaces = new HashMap<String, String>();

	public PrependTriple(Triple triple, RDFEventReader reader) {
		super(reader);
		this.triple = triple;
		if (triple.getSubject().isCURIE()) {
			CURIE subj = triple.getSubject().asCURIE();
			namespaces.put(subj.getPrefix(), subj.getNamespaceURI());
		}
		if (triple.getPredicate().isCURIE()) {
			CURIE pred = triple.getPredicate().asCURIE();
			namespaces.put(pred.getPrefix(), pred.getNamespaceURI());
		}
		if (triple.getObject().isCURIE()) {
			CURIE obj = triple.getObject().asCURIE();
			namespaces.put(obj.getPrefix(), obj.getNamespaceURI());
		}
	}

	@Override
	protected void process(RDFEvent next) throws RDFParseException {
		if (begining && triple != null && !next.isStartDocument()
				&& !next.isBase()) {
			if (next.isNamespace()) {
				namespaces.remove(next.asNamespace().getPrefix());
			} else if (!namespaces.isEmpty()) {
				for (Map.Entry<String, String> e : namespaces.entrySet()) {
					add(new Namespace(e.getKey(), e.getValue()));
				}
				namespaces.clear();
			}
			if (next.isEndDocument()) {
				begining = false;
				add(triple);
			} else if (next.isEndSubject()) {
				begining = false;
				add(triple);
			} else if (next.isTriplePattern()) {
				begining = false;
				add(triple);
			}
		}
		add(next);
	}

}
