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
package org.callimachusproject.helpers;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleUtil;

public class GraphPatternBuilder extends RDFHandlerBase {
	private StringBuilder sb = new StringBuilder();
	private boolean empty = true;

	public String toSPARQLQuery() {
		return sb.toString();
	}

	public String toString() {
		return sb.toString();
	}

	public boolean isEmpty() {
		return empty;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		sb.setLength(0);
		sb.append("{\n");
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		sb.append("}\n");
		StringBuilder qry = new StringBuilder(32 + sb.length() * 2);
		qry.append("CONSTRUCT ").append(sb);
		qry.append("WHERE ").append(sb);
		sb = qry;
	}

	public void handleStatement(Statement st) throws RDFHandlerException {
		empty = false;
		writeResource(sb, st.getSubject());
		sb.append(" ");
		writeURI(sb, st.getPredicate());
		sb.append(" ");
		writeValue(sb, st.getObject());
		sb.append(" .\n");
	}

	private void writeValue(StringBuilder sb, Value val) {
		if (val instanceof Resource) {
			writeResource(sb, (Resource) val);
		} else {
			writeLiteral(sb, (Literal) val);
		}
	}

	private void writeResource(StringBuilder sb, Resource res) {
		if (res instanceof URI) {
			writeURI(sb, (URI) res);
		} else {
			writeBNode(sb, (BNode) res);
		}
	}

	private void writeURI(StringBuilder sb, URI uri) {
		sb.append("<");
		sb.append(TurtleUtil.encodeURIString(uri.stringValue()));
		sb.append(">");
	}

	private void writeBNode(StringBuilder sb, BNode bNode) {
		sb.append("?").append(bNode.stringValue());
	}

	private void writeLiteral(StringBuilder sb, Literal lit) {
		String label = lit.getLabel();

		if (label.indexOf('\n') > 0 || label.indexOf('\r') > 0
				|| label.indexOf('\t') > 0) {
			// Write label as long string
			sb.append("\"\"\"");
			sb.append(TurtleUtil.encodeLongString(label));
			sb.append("\"\"\"");
		} else {
			// Write label as normal string
			sb.append("\"");
			sb.append(TurtleUtil.encodeString(label));
			sb.append("\"");
		}

		if (lit.getDatatype() != null) {
			// Append the literal's datatype
			sb.append("^^");
			writeURI(sb, lit.getDatatype());
		} else if (lit.getLanguage() != null) {
			// Append the literal's language
			sb.append("@");
			sb.append(lit.getLanguage());
		}
	}
}