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

package org.callimachusproject.stream;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.VarOrIRI;
import org.openrdf.model.vocabulary.RDF;

/**
 * Writes out RDF triple patterns as SPARQL.
 * 
 * @author James Leigh
 *
 */
public class SPARQLWriter implements Closeable {

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
	private boolean filtering;
	private int builtin;

	public SPARQLWriter(Writer writer) {
		this.writer = writer;
	}

	public void close() throws IOException {
		writer.close();
	}

	public void write(RDFEvent event) throws IOException {
		if (event.isFilter() && !filtering) {
			indent(indent);
			writer.append("FILTER ");
			filtering = true;
		} else if (!event.isFilter() && filtering) {
			writer.append("\n");
			filtering = false;
		}
		if (event.isBase()) {
			base = event.asBase().getBase();
		} else if (event.isNamespace()) {
			namespaces.put(event.asNamespace().getPrefix(), event.asNamespace().getNamespaceURI());
		} else if (event.isStartConstruct()) {
			if (base != null) {
				writer.append("BASE <");
				writer.append(base);
				writer.append(">\n");
			}
			if (!namespaces.isEmpty()) {
				for (Map.Entry<String, String> e : namespaces.entrySet()) {
					writer.append("PREFIX ");
					writer.append(e.getKey());
					writer.append(":<");
					writer.append(e.getValue());
					writer.append(">\n");
				}
			}
			indent(indent);
			writer.append("CONSTRUCT {\n");
			indent++;
		} else if (event.isEndConstruct()) {
			indent--;
			indent(indent);
			writer.append("}\n");
		} else if (event.isAsk()) {
			if (base != null) {
				writer.append("BASE <");
				writer.append(base);
				writer.append(">\n");
			}
			if (!namespaces.isEmpty()) {
				for (Map.Entry<String, String> e : namespaces.entrySet()) {
					writer.append("PREFIX ");
					writer.append(e.getKey());
					writer.append(":<");
					writer.append(e.getValue());
					writer.append(">\n");
				}
			}
			indent(indent);
			writer.append("ASK\n");
		} else if (event.isStartWhere()) {
			indent(indent);
			writer.append("WHERE {\n");
			indent++;
		} else if (event.isEndWhere()) {
			indent--;
			indent(indent);
			writer.append("}\n");
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
			writer.append(event.asGraph().getGraph().toString());
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
			writer.append(tp.getSubject().toString());
			writer.append(" ");
			VarOrIRI pred = tp.getPredicate();
			if (pred.isIRI() && pred.stringValue().equals(RDFTYPE)) {
				writer.append("a");
			} else {
				writer.append(pred.toString());
			}
			writer.append(" ");
			writer.append(tp.getObject().toString());
			writer.append(" .\n");
		} else if (event.isStartBuiltInCall()) {
			writer.append(event.asBuiltInCall().getFunction());
			writer.append("(");
			builtin++;
		} else if (event.isEndBuiltInCall()) {
			writer.append(")");
			builtin--;
		} else if (event.isExpression()) {
			if (builtin > 0 && !previous.isStartBuiltInCall()) {
				writer.append(", ");
			}
			writer.append(event.asExpression().getTerm().toString());
		}
		previous = event;
	}

	private void indent(int indent) throws IOException {
		for (int i = 0; i < indent; i++) {
			writer.append(" ");
		}
	}

}
