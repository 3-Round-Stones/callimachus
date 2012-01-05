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

import java.util.Iterator;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;

/**
 * Converts {@link Iterable} into {@link RDFEventReader}.
 * 
 * @author James Leigh
 *
 */
public class IterableRDFEventReader extends RDFEventReader {
	private Iterable<? extends RDFEvent> list;
	private Iterator<? extends RDFEvent> iter;

	public IterableRDFEventReader(Iterable<? extends RDFEvent> list) {
		this.list = list;
		this.iter = list.iterator();
	}

	public String toString() {
		return list.toString();
	}

	@Override
	public void close() throws RDFParseException {
		// no-op
	}

	@Override
	protected RDFEvent take() throws RDFParseException {
		if (iter.hasNext())
			return iter.next();
		return null;
	}

}
