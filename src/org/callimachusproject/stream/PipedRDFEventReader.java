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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;

public abstract class PipedRDFEventReader extends RDFEventReader {
	private RDFEventReader reader;
	private Queue<RDFEvent> queue = new LinkedList<RDFEvent>();

	public PipedRDFEventReader(RDFEventReader reader) {
		assert reader != null;
		this.reader = reader;
	}

	public String toString() {
		return getClass().getSimpleName() + " " + reader.toString();
	}

	@Override
	public void close() throws RDFParseException {
		reader.close();
	}

	protected final RDFEvent take() throws RDFParseException {
		while (queue.isEmpty()) {
			if (!reader.hasNext())
				return null;
			process(reader.next());
		}
		return queue.remove();
	}

	protected final void add(RDFEvent next) {
		queue.add(next);
	}

	protected final boolean addAll(Collection<? extends RDFEvent> c) {
		return queue.addAll(c);
	}

	protected final RDFEvent peekNext() throws RDFParseException {
		return reader.peek();
	}

	protected abstract void process(RDFEvent next) throws RDFParseException;

}
