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
package org.callimachusproject.rdfa;

import org.callimachusproject.rdfa.events.RDFEvent;

public abstract class RDFEventReader {
	private RDFEvent next;

	public final boolean hasNext() throws RDFParseException {
		if (next == null)
			return (next = take()) != null;
		return true;
	}

	public final RDFEvent peek() throws RDFParseException {
		if (next == null)
			return next = take();
		return next;
	}

	public final RDFEvent next() throws RDFParseException {
		if (next == null)
			return take();
		try {
			return next;
		} finally {
			next = null;
		}
	}

	public abstract void close() throws RDFParseException;

	protected abstract RDFEvent take() throws RDFParseException;

}
