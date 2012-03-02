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

import java.util.List;
import java.util.ListIterator;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;

/**
 * Converts {@link List} into {@link RDFEventReader}.
 * 
 * @author James Leigh
 *
 */
public class RDFEventIterator implements RDFEventReader, ListIterator<RDFEvent> {
	private List<? extends RDFEvent> list;
	private ListIterator<RDFEvent> iter;

	public RDFEventIterator(List<RDFEvent> list, ListIterator<RDFEvent> listIterator) {
		this.list = list;
		this.iter = listIterator;
	}

	public String toString() {
		return list.toString();
	}

	public void close() throws RDFParseException {
		// no-op
	}

	public RDFEvent peek() throws RDFParseException {
		if (iter.hasNext()) {
			iter.next();
			return iter.previous();
		}
		return null;
	}

	public boolean hasNext() {
		return iter.hasNext();
	}

	public RDFEvent next(){
		if (iter.hasNext())
			return iter.next();
		return null;
	}

	public boolean hasPrevious() {
		return iter.hasPrevious();
	}

	public RDFEvent previous(){
		if (iter.hasPrevious())
			return iter.previous();
		return null;
	}

	public int nextIndex() {
		return iter.nextIndex();
	}

	public int previousIndex() {
		return iter.previousIndex();
	}

	public void remove() {
		iter.remove();
	}

	public void set(RDFEvent e) {
		iter.set(e);
	}

	public void add(RDFEvent e) {
		iter.add(e);
	}

}
