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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Triple;

public class ReducedTripleReader extends PipedRDFEventReader {
	private Map<Triple, Object> set = new LinkedHashMap<Triple, Object>() {
		private static final long serialVersionUID = -8926173487624544845L;

		protected boolean removeEldestEntry(Entry<Triple, Object> eldest) {
			return size() > 1024;
		}
	};

	public ReducedTripleReader(RDFEventReader reader) {
		super(reader);
	}

	@Override
	protected void process(RDFEvent next) throws RDFParseException {
		if (!next.isTriple()) {
			add(next);
		} else if (!set.containsKey(next)) {
			set.put(next.asTriple(), Boolean.TRUE);
			add(next);
		}
	}

}
