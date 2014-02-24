/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.engine.helpers;

import java.util.Collection;
import java.util.LinkedList;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;

public class RDFEventList extends LinkedList<RDFEvent> {
	private static final long serialVersionUID = -7053498842173406118L;

	public RDFEventList() {
		super();
	}

	public RDFEventList(Collection<RDFEvent> events) {
		super(events);
	}

	/**
	 * 
	 * @param reader is closed
	 * @throws RDFParseException 
	 */
	public RDFEventList(RDFEventReader reader) throws RDFParseException {
		try {
			addAll(reader);
		} finally {
			reader.close();
		}
	}

	/**
	 * 
	 * @param reader is not closed
	 * @throws RDFParseException
	 */
	public void addAll(RDFEventReader reader) throws RDFParseException {
		while (reader.hasNext()) {
			add(reader.next());
		}
	}

	public RDFEventIterator iterator() {
		return new RDFEventIterator(this, super.listIterator());
	}

	public RDFEventIterator listIterator() {
		return new RDFEventIterator(this, super.listIterator());
	}

	public RDFEventIterator listIterator(int index) {
		return new RDFEventIterator(this, super.listIterator(index));
	}

}
