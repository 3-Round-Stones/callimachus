package org.callimachusproject.io;

import java.io.IOException;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.trig.TriGParser;
import org.openrdf.rio.turtle.TurtleUtil;

public class SparqlInsertDataParser extends TriGParser {

	public SparqlInsertDataParser() {
		this(ValueFactoryImpl.getInstance());
	}

	public SparqlInsertDataParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	protected void reportDropGraph(URI graph) throws RDFHandlerException {
	};

	protected void reportDropDefault() throws RDFHandlerException {
	};

	protected void reportDropNamed() throws RDFHandlerException {
	};

	protected void reportDropAll() throws RDFHandlerException {
	};

	@Override
	protected void parseStatement() throws IOException, RDFParseException,
			RDFHandlerException {
		String directive = parseWord("@prefix".length());

		if (directive.startsWith("@")) {
			parseDirective(directive);
			skipWSC();
			verifyCharacterOrFail(readCodePoint(), ".");
		} else if ((directive.length() >= 6 && directive.substring(0, 6)
				.equalsIgnoreCase("prefix"))
				|| (directive.length() >= 4 && directive.substring(0, 4)
						.equalsIgnoreCase("base"))) {
			parseDirective(directive);
			skipWSC();
			// SPARQL BASE and PREFIX lines do not end in .
		} else if ("DROP".equalsIgnoreCase(directive)) {
			parseDrop(directive);
		} else if ("INSERT".equalsIgnoreCase(directive)) {
			parseInsertData(directive);
		} else if ("GRAPH".equalsIgnoreCase(directive)) {
			// Do not unread the directive if it was SPARQL GRAPH
			// Just continue with TriG parsing at this point
			skipWSC();

			parseGraph();
		} else {
			unread(directive);
			parseGraph();
		}
	}

	private void parseDrop(String directive) throws RDFHandlerException, IOException, RDFParseException {
		String silent = parseWord("SILENT".length());
		if (!"SILENT".equalsIgnoreCase(silent)) {
			unread(silent);
		}
		String all = parseWord("DEFAULT".length());
		if ("GRAPH".equalsIgnoreCase(all)) {
			int c = skipWSC();
			Value value = parseValue();
			if (value instanceof URI) {
				reportDropGraph((URI) value);
			} else {
				StringBuilder msg = new StringBuilder(32);
				msg.append("Expected '<', found '");
				msg.append(c);
				msg.append("'");
				reportFatalError(msg.toString());
			}
		} else if ("DEFAULT".equalsIgnoreCase(all)) {
			reportDropDefault();
		} else if ("NAMED".equalsIgnoreCase(all)) {
			reportDropNamed();
		} else if ("ALL".equalsIgnoreCase(all)) {
			reportDropAll();
		} else {
			StringBuilder msg = new StringBuilder(32);
			msg.append("Expected GRAPH, DEFALUT, NAMED, or ALL, found '");
			msg.append(all);
			msg.append("'");
			reportFatalError(msg.toString());
		}
		int c = skipWSC();
		if (c == ';') {
			readCodePoint();
		}
	}

	private void parseInsertData(String directive) throws IOException,
			RDFHandlerException, RDFParseException {
		verifyWordOrFail("DATA");
		verifyWordOrFail("{");
		String graph = parseWord("GRAPH".length());
		do {
			if ("GRAPH".equalsIgnoreCase(graph)) {
				skipWSC();
				parseGraph();
			} else if (graph.charAt(0) == '}') {
				unread(graph);
			} else {
				unread(graph);
				parseTriples();
				int c = skipWSC();
				if (c == '.') {
					readCodePoint();
				}
			}
			graph = parseWord("GRAPH".length());
		} while (graph.charAt(0) != '}');
		unread(graph);
		verifyWordOrFail("}");
		int c = skipWSC();
		if (c == ';') {
			readCodePoint();
		}
	}

	private String parseWord(int size) throws IOException, RDFHandlerException {
		StringBuilder sb = new StringBuilder(size);
		int c = skipWSC();
		do {
			c = readCodePoint();
			if (c == -1 || c == ';' && sb.length() > 0 || TurtleUtil.isWhitespace(c)) {
				unread(c);
				break;
			}
			sb.append((char) c);
		} while (sb.length() < size);
		return sb.toString();
	}

	private void verifyWordOrFail(String expected) throws RDFParseException,
			IOException, RDFHandlerException {
		final String supplied = parseWord(expected.length());
		if (!expected.equalsIgnoreCase(supplied)) {
			StringBuilder msg = new StringBuilder(32);
			msg.append("Expected ").append(expected);
			msg.append(", found '");
			msg.append(supplied);
			msg.append("'");
			reportFatalError(msg.toString());
		}
	}
}