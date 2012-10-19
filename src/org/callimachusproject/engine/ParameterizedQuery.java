package org.callimachusproject.engine;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.EmptyIteration;
import info.aduna.text.ASCIIUtil;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.callimachusproject.engine.model.TermFactory;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.turtle.TurtleUtil;

public class ParameterizedQuery {
	private static final Pattern CACHE_CONTROL = Pattern
			.compile(
					"(?:^|\n|\r).*#[ \t]*@Cache-Control[ \t]*(?::[ \t]*)?([\\w ,=\\-\"]+)[ \t]*(?:$|\n|\r)",
					Pattern.CASE_INSENSITIVE);
	private static final Pattern VIEW_TEMPLATE = Pattern
			.compile(
					"(?:^|\n|\r).*#[ \t]*@view[ \t]*(?::[ \t]*)?([^\\s'\"<>]+)[ \t]*(?:$|\n|\r)",
					Pattern.CASE_INSENSITIVE);
	private static final Pattern PARAM_EXPRESSION = Pattern
			.compile("(?<!\\\\)\\$\\{([^}]*)\\}");
	private static ValueFactory vf = ValueFactoryImpl.getInstance();
	private final String sparql;
	private final String systemId;
	private final Map<String, String> prefixes;
	private final List<String> bindingNames;
	private final Map<String, Value> bindings;
	private final TermFactory tf;

	ParameterizedQuery(String sparql, String systemId,
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

	public boolean isParameterPresent() {
		return !bindings.isEmpty();
	}

	public String getCacheControl() {
		StringBuilder sb = new StringBuilder();
		Matcher matcher = CACHE_CONTROL.matcher(sparql);
		while (matcher.find()) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(matcher.group(1));
		}
		if (sb.length() == 0)
			return null;
		return sb.toString();
	}

	public String getViewTemplate() {
		Matcher matcher = VIEW_TEMPLATE.matcher(sparql);
		if (matcher.find())
			return tf.resolve(matcher.group(1));
		return null;
	}

	public String prepare() throws IllegalArgumentException {
		Map<String, ?> parameters = Collections.emptyMap();
		return prepare(parameters);
	}

	public String prepare(Map<String, ?> parameters)
			throws IllegalArgumentException {
		String sparql = this.sparql;
		if (isExpressionPresent()) {
			try {
				sparql = inlineExpressions(sparql, parameters);
			} catch (QueryEvaluationException e) {
				throw new IllegalArgumentException(e);
			} catch (MalformedQueryException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return appendBindings(sparql, parameters);
	}

	public TupleQueryResult evaluate(RepositoryConnection con) throws RepositoryException,
			MalformedQueryException, IllegalArgumentException,
			QueryEvaluationException {
		return evaluate(null, con);
	}

	public TupleQueryResult evaluate(Map<String, ?> parameters,
			RepositoryConnection con) throws RepositoryException,
			MalformedQueryException, IllegalArgumentException,
			QueryEvaluationException {
		TupleQuery qry = con.prepareTupleQuery(QueryLanguage.SPARQL,
				prepare(parameters), systemId);
		return evaluate(parameters, qry);
	}

	public TupleQueryResult evaluate(TupleQuery qry)
			throws QueryEvaluationException {
		return evaluate(null, qry);
	}

	public TupleQueryResult evaluate(Map<String, ?> parameters, TupleQuery qry)
			throws QueryEvaluationException {
		if (isParameterPresent()) {
			for (String name : bindingNames) {
				Value[] values = getValues(parameters, name);
				if (values.length == 1) {
					if (values[0] != null) {
						qry.setBinding(name, values[0]);
					}
				}
			}
		}
		return qry.evaluate();
	}

	private boolean isExpressionPresent() {
		return this.sparql.contains("${");
	}

	private String inlineExpressions(String sparql, Map<String, ?> parameters)
			throws QueryEvaluationException, MalformedQueryException {
		StringBuilder sb = new StringBuilder();
		Matcher m = PARAM_EXPRESSION.matcher(sparql);
		String prologue = getPrologue();
		int position = 0;
		while (m.find()) {
			String expression = m.group(1);
			Value value = evaluate(prologue, expression, parameters);
			sb.append(sparql, position, m.start());
			sb.append(writeValue(value));
			position = m.end();
		}
		sb.append(sparql, position, sparql.length());
		return sb.toString();
	}

	private Value evaluate(String prologue, String expression,
			Map<String, ?> parameters) throws MalformedQueryException,
			QueryEvaluationException {
		Literal defaultValue = vf.createLiteral("0", XMLSchema.INTEGER);
		String select = prologue + "SELECT (" + expression
				+ " AS ?_value) {} LIMIT 1";
		String qry = appendBindings(select, parameters);
		TupleExpr expr = new SPARQLParser().parseQuery(qry, systemId)
				.getTupleExpr();
		QueryBindingSet bindings = new QueryBindingSet();
		if (isParameterPresent()) {
			for (String name : bindingNames) {
				Value[] values = getValues(parameters, name);
				if (values.length == 1) {
					if (values[0] != null) {
						bindings.setBinding(name, values[0]);
					}
				}
			}
		}
		CloseableIteration<BindingSet, QueryEvaluationException> iter;
		iter = new EvaluationStrategyImpl(new TripleSource() {
			public ValueFactory getValueFactory() {
				return vf;
			}

			public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(
					Resource subj, URI pred, Value obj, Resource... contexts)
					throws QueryEvaluationException {
				return new EmptyIteration<Statement, QueryEvaluationException>();
			}
		}).evaluate(expr, bindings);
		try {
			if (!iter.hasNext())
				return defaultValue;
			Value value = iter.next().getValue("_value");
			if (value == null)
				return defaultValue;
			return value;
		} finally {
			iter.close();
		}
	}

	private String getPrologue() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : prefixes.entrySet()) {
			sb.append("PREFIX ").append(e.getKey());
			sb.append(":<").append(e.getValue()).append(">\n");
		}
		return sb.toString();
	}

	private String appendBindings(String sparql, Map<String, ?> parameters) {
		List<String> bindingNames = getNonFunctionalParameterNames(parameters);
		if (bindingNames.isEmpty())
			return sparql;
		StringBuilder sb = new StringBuilder(sparql);
		sb.append("\nBINDINGS");
		for (String name : bindingNames) {
			sb.append(" $").append(name);
		}
		sb.append(" {\n");
		for (List<Value> values : getParameterBindingValues(bindingNames,
				parameters)) {
			sb.append("\t(");
			for (Value value : values) {
				if (value == null) {
					sb.append("UNDEF");
				} else {
					sb.append(writeValue(value));
				}
				sb.append(" ");
			}
			sb.append(")\n");
		}
		sb.append("}\n");
		return sb.toString();
	}

	private List<String> getNonFunctionalParameterNames(
			Map<String, ?> parameters) {
		if (!isParameterPresent())
			return Collections.emptyList();
		List<String> list = new ArrayList<String>(bindingNames.size());
		for (String name : bindingNames) {
			Value[] values = getValues(parameters, name);
			if (values.length > 1) {
				list.add(name);
			}
		}
		return list;
	}

	private List<List<Value>> getParameterBindingValues(
			List<String> bindingNames, Map<String, ?> parameters) {
		List<List<Value>> bindingValues = Collections.singletonList(Collections
				.<Value> emptyList());
		for (String name : bindingNames) {
			Value[] values = getValues(parameters, name);
			if (values.length == 1)
				continue; // functional
			List<List<Value>> list;
			int size = bindingValues.size() + values.length - 1;
			list = new ArrayList<List<Value>>(size);
			for (Value value : values) {
				appendBinding(bindingValues, value, list);
			}
			bindingValues = list;
		}
		return bindingValues;
	}

	private Value[] getValues(Map<String, ?> parameters, String name) {
		String[] strings = getStrings(parameters, name);
		if (strings == null || strings.length == 0)
			return new Value[] { bindings.get(name) };
		Value[] values = new Value[strings.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = resolve(name, strings[i]);
			if (values[i] == null)
				throw new IllegalArgumentException("Invalid parameter value: "
						+ strings[i]);
		}
		return values;
	}

	private String[] getStrings(Map<String, ?> parameters, String name) {
		if (parameters == null)
			return null;
		Object value = parameters.get(name);
		if (value == null)
			return null;
		if (value instanceof String[])
			return (String[]) value;
		if (!value.getClass().isArray())
			return new String[] { value.toString() };
		String[] result = new String[Array.getLength(value)];
		for (int i = 0; i < result.length; i++) {
			Object element = Array.get(value, i);
			result[i] = element == null ? null : element.toString();
		}
		return result;
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
			return vf.createURI(tf.resolve(value));
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

	private CharSequence writeValue(Value value) {
		if (value instanceof Literal) {
			return writeLiteral((Literal) value);
		} else {
			return writeURI(value);
		}
	}

	private CharSequence writeLiteral(Literal lit) {
		StringBuilder sb = new StringBuilder();
		if (XMLSchema.INTEGER.equals(lit.getDatatype())) {
			try {
				return new BigInteger(lit.getLabel()).toString();
			} catch (NumberFormatException e) {
				// continue
			}
		}
		sb.append("\"");
		String label = lit.stringValue();
		sb.append(TurtleUtil.encodeString(label));
		sb.append("\"");
		if (lit.getLanguage() != null) {
			// Append the literal's language
			sb.append("@");
			sb.append(lit.getLanguage());
		} else if (lit.getDatatype() != null) {
			// Append the literal's datatype
			sb.append("^^");
			sb.append(writeURI(lit.getDatatype()));
		}
		return sb;
	}

	private CharSequence writeURI(Value value) {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		String uri = value.stringValue();
		sb.append(TurtleUtil.encodeURIString(uri));
		sb.append(">");
		return sb;
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
			return vf.createURI(tf.resolve(ref));
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
