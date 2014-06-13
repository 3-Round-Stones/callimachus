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
package org.callimachusproject.rdfa.test;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.helpers.AbstractRDFEventReader;
import org.callimachusproject.engine.model.GraphNodePath;

public class SerializeRDFReader extends AbstractRDFEventReader {
	private RDFEventReader delegate;

	public SerializeRDFReader(RDFEventReader delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() throws RDFParseException {
		delegate.close();
	}

	@Override
	protected RDFEvent take() throws RDFParseException {
		RDFEvent next = delegate.next();
		if (next == null)
			return next;
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		sb.append(next.getClass().getSimpleName());
		sb.append("(");
		if (next.isStart()) {
			sb.append("true");
		} else if (next.isEnd()) {
			sb.append("false");
		}
		if (next.isBase()) {
			sb.append("\"");
			sb.append(next.asBase().getBase());
			sb.append("\"");
		} else if (next.isNamespace()) {
			sb.append("\"");
			sb.append(next.asNamespace().getPrefix());
			sb.append("\",\"");
			sb.append(next.asNamespace().getNamespaceURI());
			sb.append("\"");
		} else if (next.isStartSubject() || next.isEndSubject()) {
			sb.append(",");
			serializeTerm(sb, next.asSubject().getSubject());
		} else if (next.isTriplePattern()) {
			TriplePattern tp = next.asTriplePattern();
			serializeTerm(sb, tp.getSubject());
			sb.append(",");
			serializeTerm(sb, tp.getProperty());
			sb.append(",");
			serializeTerm(sb, tp.getObject());
			sb.append(",");
			sb.append(tp.isInverse());
		} else if (next.isComment()) {
			sb.append("\"");
			sb.append(next.asComment().getComment());
			sb.append("\"");
		}
		sb.append("),");
		System.out.println(sb.toString());
		return next;
	}

	private void serializeTerm(StringBuilder sb, GraphNodePath node) {
		sb.append("new ");
		sb.append(node.getClass().getSimpleName());
		sb.append("(\"");
		sb.append(node.stringValue());
		sb.append("\")");
	}

}
