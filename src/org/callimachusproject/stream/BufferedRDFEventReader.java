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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;

/**
 * Wraps an {@link RDFEventReader} and provides a {@link Queue} to buffer the output.
 * Provides a {@link Queue} to buffer the input allowing look-ahead to arbitrary depth.
 * 
 * @author James Leigh
 * @author Steve Battle
 *
 */
public abstract class BufferedRDFEventReader extends RDFEventReader {
	private RDFEventReader reader;
	private Queue<RDFEvent> queue = new LinkedList<RDFEvent>();
	private Queue<RDFEvent> buffer = new LinkedList<RDFEvent>();

	public BufferedRDFEventReader(RDFEventReader reader) {
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
			if (!buffer.isEmpty()) process(buffer.remove());
			else if (reader.hasNext()) process(reader.next());
			else return null;
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
		if (buffer.isEmpty()) return reader.peek();
		else return buffer.peek();
	}
	
	protected final RDFEvent peek(int lookAhead) throws RDFParseException {
		while (buffer.size()<=lookAhead) {
			buffer.add(reader.next());
		}
		if (buffer.size()==lookAhead) return reader.peek();
		Iterator<RDFEvent> it = buffer.iterator();
		RDFEvent e = null;
		while (lookAhead-->=0) e = (RDFEvent) it.next(); 
		return e;
	}

	protected abstract void process(RDFEvent next) throws RDFParseException;

}
