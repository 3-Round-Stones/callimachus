/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.model.VarOrIRI;
import org.callimachusproject.engine.model.VarOrTerm;
import org.openrdf.model.vocabulary.RDF;

/**
 * Writes out RDF triple patterns as SPARQL.
 * 
 * @author James Leigh
 * @author Steve Battle
 *
 */
public class SPARQLWriter implements Closeable, Flushable {

	private static final String RDFTYPE = RDF.TYPE.stringValue();

	public static String toSPARQL(RDFEventReader reader) throws RDFParseException,
			IOException {
		if (reader == null)
			return null;
		try {
			StringWriter str = new StringWriter();
			SPARQLWriter writer = new SPARQLWriter(str);
			while (reader.hasNext()) {
				writer.write(reader.next());
			}
			return str.toString();
		} finally {
			reader.close();
		}
	}

	private Writer writer;
	private String base;
	private Map<String, String> namespaces = new HashMap<String, String>();
	private int indent;
	private RDFEvent previous;
	//private boolean filtering;
	private int builtin;

	public SPARQLWriter(Writer writer) {
		this.writer = writer;
	}

	public void flush() throws IOException {
		flushInternalState();
		writer.flush();
	}

	public void close() throws IOException {
		writer.close();
	}

	public void write(RDFEvent event) throws IOException {
		if (event.isExpression()) {
			if (builtin > 0 && !previous.isStart() && !event.isEnd()) {
				writer.append(", ");
			}
		}
		if (event.isStartFilter()) {
			indent(indent);
			writer.append("FILTER (");
		} else if (event.isEndFilter()) {
			writer.append(")\n");
		} else if (event.isBase()) {
			base = event.asBase().getBase();
		} else if (event.isNamespace()) {
			namespaces.put(event.asNamespace().getPrefix(), event.asNamespace().getNamespaceURI());
		} else if (event.isStartConstruct()) {
			flushInternalState();
			indent(indent);
			writer.append("CONSTRUCT {\n");
			indent++;
		} else if (event.isEndConstruct()) {
			indent--;
			indent(indent);
			writer.append("}\n");
		} else if (event.isAsk()) {
			flushInternalState();
			indent(indent);
			writer.append("ASK\n");
		} else if (event.isSelect()) {
			flushInternalState();
			indent(indent);
			writer.append("SELECT REDUCED *\n");
		} else if (event.isStartWhere()) {
			indent(indent);
			writer.append("WHERE {\n");
			indent++;
		} else if (event.isEndWhere()) {
			indent--;
			indent(indent);
			writer.append("}\n");
		} else if (event.isStartExists()) {
			indent(indent);
			writer.append("EXISTS {\n");
			indent++;
		} else if (event.isEndExists()) {
			indent--;
			indent(indent);
			writer.append("}");
		} else if (event.isStartGroup()) {
			indent(indent);
			writer.append("{\n");
			indent++;
		} else if (event.isEndGroup()) {
			indent--;
			indent(indent);
			writer.append("}\n");
		} else if (event.isStartGraph()) {
			indent(indent);
			writer.append("GRAPH ");
			writer.append(term(event.asGraph().getGraph()));
			writer.append(" {\n");
			indent++;
		} else if (event.isEndGraph()) {
			indent--;
			indent(indent);
			writer.append("}\n");
		} else if (event.isUnion()) {
			indent(indent);
			writer.append("UNION\n");
		} else if (event.isStartOptional()) {
			indent(indent);
			writer.append("OPTIONAL {\n");
			indent++;
		} else if (event.isEndOptional()) {
			indent--;
			indent(indent);
			writer.append("}\n");
		} else if (event.isTriplePattern()) {
			TriplePattern tp = event.asTriplePattern();
			indent(indent);
			writer.append(term(tp.getSubject()));
			writer.append(" ");
			VarOrIRI pred = tp.getPredicate();
			if (pred.isIRI() && pred.stringValue().equals(RDFTYPE)) {
				writer.append("a");
			} else {
				writer.append(term(pred));
			}
			writer.append(" ");
			writer.append(term(tp.getObject()));
			writer.append(" .\n");
		} else if (event.isStartBuiltInCall()) {
			writer.append(event.asBuiltInCall().getFunction());
			writer.append("(");
			builtin++;
		} else if (event.isEndBuiltInCall()) {
			writer.append(")");
			builtin--;
		} else if (event.isVarOrTerm()) {
			writer.append(term(event.asVarOrTerm()));
		}
		else if (event.isStart() || event.isEnd()) {
			// unknown block
		}
		else  {
			writer.append(event.toString());
			writer.append("\n");
		}
		previous = event;
	}

	private CharSequence term(VarOrTerm term) {
		return term.toString();
	}

	private void indent(int indent) throws IOException {
		for (int i = 0; i < indent; i++) {
			writer.append(" ");
		}
	}

	private void flushInternalState() throws IOException {
		if (base != null) {
			writer.append("BASE <");
			writer.append(base);
			writer.append(">\n");
			base = null;
		}
		if (!namespaces.isEmpty()) {
			for (Map.Entry<String, String> e : namespaces.entrySet()) {
				writer.append("PREFIX ");
				writer.append(e.getKey());
				writer.append(":<");
				writer.append(e.getValue());
				writer.append(">\n");
			}
			namespaces.clear();
		}
	}

}
