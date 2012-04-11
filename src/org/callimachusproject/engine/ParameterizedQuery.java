package org.callimachusproject.engine;

import info.aduna.text.ASCIIUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.callimachusproject.engine.model.TermFactory;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.turtle.TurtleUtil;

public class ParameterizedQuery {
	private static ValueFactory vf = ValueFactoryImpl.getInstance();
	private final String sparql;
	private final String systemId;
	private final Map<String, String> prefixes;
	private final List<String> bindingNames;
	private final Map<String, Value> bindings;
	private final TermFactory tf;

	public ParameterizedQuery(String sparql, String systemId,
			Map<String, String> prefixes, Map<String, Value> bindings) {
		assert sparql != null;
		assert systemId != null;
		assert bindings != null;
		this.sparql = sparql;
		this.systemId = systemId;
		this.prefixes = prefixes;
		this.bindingNames = new ArrayList<String>(bindings.keySet());
		this.bindings = bindings;
		this.tf = TermFactory.newInstance(systemId);
	}

	public String toString() {
		return sparql;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + sparql.hashCode();
		result = prime * result + systemId.hashCode();
		result = prime * result + bindings.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterizedQuery other = (ParameterizedQuery) obj;
		if (!sparql.equals(other.sparql))
			return false;
		if (!systemId.equals(other.systemId))
			return false;
		if (!bindings.equals(other.bindings))
			return false;
		return true;
	}

	public String prepare(Map<String, String[]> parameters) throws IllegalArgumentException {
		if (bindings.isEmpty())
			return sparql;
		StringBuilder sb = new StringBuilder(sparql);
		sb.append("\nBINDINGS");
		for (String name : bindingNames) {
			sb.append(" $").append(name);
		}
		sb.append(" {\n");
		for (List<Value> values : getParameterBindingValues(parameters)) {
			sb.append("\t(");
			for (Value value : values) {
				if (value == null) {
					sb.append("UNDEF");
				} else if (value instanceof Literal) {
					writeLiteral(sb, value);
				} else {
					writeURI(sb, value);
				}
				sb.append(" ");
			}
			sb.append(")\n");
		}
		sb.append("}\n");
		return sb.toString();
	}

	private List<List<Value>> getParameterBindingValues(
			Map<String, String[]> parameters) {
		List<List<Value>> bindingValues = Collections.singletonList(Collections
				.<Value> emptyList());
		for (String name : bindingNames) {
			String[] strings = parameters == null ? null : parameters.get(name);
			if (strings == null || strings.length == 0) {
				List<List<Value>> list;
				list = new ArrayList<List<Value>>(bindingValues.size());
				appendBinding(bindingValues, bindings.get(name), list);
				bindingValues = list;
			} else {
				List<List<Value>> list;
				int size = bindingValues.size() + strings.length - 1;
				list = new ArrayList<List<Value>>(size);
				for (String string : strings) {
					Value value = resolve(name, string);
					if (value == null)
						throw new IllegalArgumentException("Invalid parameter value: " + string);
					appendBinding(bindingValues, value, list);
				}
				bindingValues = list;
			}
		}
		return bindingValues;
	}

	private Value resolve(String name, String value) {
		Value sample = bindings.get(name);
		if (sample instanceof Literal) {
			Literal lit = (Literal) sample;
			if (lit.getLanguage() != null) {
				return vf.createLiteral(value, lit.getLanguage());
			} else if (lit.getDatatype() != null) {
				return vf.createLiteral(value, lit.getDatatype());
			} else {
				return vf.createLiteral(value);
			}
		} else if (sample instanceof URI) {
			return vf.createURI(tf.reference(value).stringValue());
		} else {
			return parseValue(value);
		}
	}

	private void appendBinding(List<List<Value>> existingBindings, Value value,
			List<List<Value>> target) {
		for (List<Value> bindings : existingBindings) {
			List<Value> set = new ArrayList<Value>(bindings.size() + 1);
			set.addAll(bindings);
			set.add(value);
			target.add(set);
		}
	}

	private void writeLiteral(StringBuilder sb, Value value) {
		Literal lit = (Literal) value;
		sb.append("\"");
		String label = value.stringValue();
		sb.append(TurtleUtil.encodeString(label));
		sb.append("\"");
		if (lit.getLanguage() != null) {
			// Append the literal's language
			sb.append("@");
			sb.append(lit.getLanguage());
		} else if (lit.getDatatype() != null) {
			// Append the literal's datatype
			sb.append("^^");
			writeURI(sb, lit.getDatatype());
		}
	}

	private void writeURI(StringBuilder sb, Value value) {
		sb.append("<");
		String uri = value.stringValue();
		sb.append(TurtleUtil.encodeURIString(uri));
		sb.append(">");
	}

	/**
	 * Parses an RDF value. This method parses uriref, qname, node ID, quoted
	 * literal, integer, double and boolean.
	 */
	private Value parseValue(String string) {
		char c = string.charAt(0);

		if (c == '<') {
			// uriref, e.g. <foo://bar>
			return parseURI(string);
		} else if (c == ':' || TurtleUtil.isPrefixStartChar(c)) {
			// qname or boolean
			return parseQNameOrBoolean(string);
		} else if (c == '_') {
			// node ID, e.g. _:n1
			return parseNodeID(string);
		} else if (c == '"') {
			// quoted literal, e.g. "foo" or """foo"""
			return parseQuotedLiteral(string);
		} else if (ASCIIUtil.isNumber(c) || c == '.' || c == '+' || c == '-') {
			// integer or double, e.g. 123 or 1.2e3
			return parseNumber(string);
		} else {
			return null;
		}
	}

	/**
	 * Parses a quoted string, optionally followed by a language tag or
	 * datatype.
	 */
	private Literal parseQuotedLiteral(String string) {
		String label = parseQuotedString(string);
		if (label == null)
			return null;

		// Check for presence of a language tag or datatype
		int idx = string.lastIndexOf('"');
		if (idx < 1)
			return null;
		if (idx == string.length() - 1)
			return vf.createLiteral(label);

		if (string.length() > idx + 2 && string.charAt(idx + 1) == '@') {
			// Read language
			return vf.createLiteral(label, string.substring(idx + 2));
		}
		if (string.length() > idx + 3 && string.charAt(idx + 1) == '^'
				&& string.charAt(idx + 2) == '^') {
			// Read datatype
			Value datatype = parseValue(string.substring(idx + 3));
			if (datatype instanceof URI)
				return vf.createLiteral(label, (URI) datatype);
		}
		return null;
	}

	/**
	 * Parses a quoted string, which is either a "normal string" or a """long
	 * string""".
	 */
	private String parseQuotedString(String string) {
		String result = null;

		// First character should be '"'
		assert string.charAt(0) == '"';

		// Check for long-string, which starts and ends with three double quotes
		if (string.length() > 6 && string.startsWith("\"\"\"")) {
			// Long string
			int idx = string.lastIndexOf("\"\"\"");
			if (idx < 1)
				return null;
			result = string.substring(3, idx);
		} else {
			// Normal string
			int idx = string.lastIndexOf('"');
			if (idx < 0)
				return null;
			result = string.substring(1, idx);
		}

		// Unescape any escape sequences
		try {
			if (result != null)
				return TurtleUtil.decodeString(result);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		return null;
	}

	private Literal parseNumber(String string) {
		if (string.contains("e") || string.contains("E"))
			return vf.createLiteral(string, XMLSchema.DOUBLE);
		if (string.contains("."))
			return vf.createLiteral(string, XMLSchema.DECIMAL);
		return vf.createLiteral(string, XMLSchema.INTEGER);
	}

	private URI parseURI(String string) {
		// First character should be '<'
		assert string.charAt(0) == '<';

		int idx = string.indexOf('>');
		if (idx < 0 || idx != string.length() - 1)
			return null;

		String uri = string.substring(1, idx);

		// Unescape any escape sequences
		try {
			String ref = TurtleUtil.decodeString(uri);
			return vf.createURI(tf.reference(ref).stringValue());
		} catch (IllegalArgumentException e) {
			// ignore
		}
		return null;
	}

	/**
	 * Parses boolean values or CURIES
	 */
	private Value parseQNameOrBoolean(String value) {
		if (value.equals("true") || value.equals("false"))
			return vf.createLiteral(value, XMLSchema.BOOLEAN);
		int idx = value.indexOf(':');
		if (idx < 0)
			return null;
		String ns = prefixes.get(value.substring(0, idx));
		if (ns == null)
			return null;
		return vf.createURI(ns + value.substring(idx + 1));
	}

	/**
	 * Parses a blank node ID, e.g. <tt>_:node1</tt>.
	 */
	private BNode parseNodeID(String string) {
		// Node ID should start with "_:"
		assert string.startsWith("_:");

		return vf.createBNode(string.substring(3));
	}

}
