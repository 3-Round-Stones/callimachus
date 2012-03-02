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
package org.callimachusproject.engine.helpers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;

/**
 * Wraps an {@link RDFEventReader} and provides a {@link Queue} to buffer the output.
 * 
 * @author James Leigh
 * @author Steve Battle
 *
 */
public abstract class RDFEventPipe extends AbstractRDFEventReader {
	private RDFEventReader reader;
	private Queue<RDFEvent> queue = new LinkedList<RDFEvent>();

	public RDFEventPipe(RDFEventReader reader) {
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
			if (reader.hasNext()) process(reader.next());
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

	protected abstract void process(RDFEvent next) throws RDFParseException;

}
