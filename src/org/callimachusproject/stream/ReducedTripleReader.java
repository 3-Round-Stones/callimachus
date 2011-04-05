/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Triple;

/**
 * Filters out any duplicate triples within a 1024 buffer.
 * 
 * @author James Leigh
 *
 */
public class ReducedTripleReader extends PipedRDFEventReader {
	private Map<Triple, Object> set = new LinkedHashMap<Triple, Object>(256, 0.75f, true) {
		private static final long serialVersionUID = -8926173487624544845L;

		protected boolean removeEldestEntry(Entry<Triple, Object> eldest) {
			return size() > 10240;
		}
	};

	public ReducedTripleReader(RDFEventReader reader) {
		super(reader);
	}

	@Override
	protected void process(RDFEvent next) throws RDFParseException {
		if (!next.isTriple()) {
			add(next);
		} else if (set.get(next) == null) {
			set.put(next.asTriple(), Boolean.TRUE);
			add(next);
		}
	}

}
