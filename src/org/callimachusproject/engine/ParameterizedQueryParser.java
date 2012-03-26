package org.callimachusproject.engine;

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.BindingSetAssignment;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.sparql.SPARQLParser;

public class ParameterizedQueryParser {

	private final class ParameterScanner extends
			QueryModelVisitorBase<MalformedQueryException> {
		private final Set<String> variables = new LinkedHashSet<String>();
		private final MapBindingSet parameters = new MapBindingSet();
		private final String systemId;
		private final String space;

		ParameterScanner(String systemId) {
			this.systemId = systemId;
			space = new ParsedURI(systemId).resolve("$").toString();
		}

		public BindingSet scan(String sparql) throws MalformedQueryException {
			ParsedQuery parsed = new SPARQLParser().parseQuery(sparql, systemId);
			if (!(parsed instanceof ParsedTupleQuery))
				throw new MalformedQueryException("Only SELECT queries are supported");
			parsed.getTupleExpr().visit(this);
			return parameters;
		}

		@Override
		public void meet(Var node) throws MalformedQueryException {
			super.meet(node);
			variables.add(node.getName());
			Value value = node.getValue();
			if (value != null) {
				meet(new ValueConstant(value));
			}
		}

		@Override
		public void meet(ValueConstant node) throws MalformedQueryException {
			super.meet(node);
			Value value = node.getValue();
			if (value instanceof Literal) {
				if (((Literal) value).getLabel().startsWith("$")) {
					String name = ((Literal) value).getLabel().substring(1);
					addParameter(name, value);
				}
			} else if (value instanceof URI) {
				String iri = value.stringValue();
				if (iri.startsWith(space)) {
					String name = iri.substring(space.length());
					addParameter(name, value);
				}
			}
		}

		@Override
		public void meet(BindingSetAssignment node) throws MalformedQueryException {
			super.meet(node);
			throw new MalformedQueryException("BINDINGS clause is not supported");
		}

		@Override
		public void meet(ProjectionElem node) throws MalformedQueryException {
			super.meet(node);
			variables.add(node.getSourceName());
			variables.add(node.getTargetName());
		}

		@Override
		public void meet(ExtensionElem node) throws MalformedQueryException {
			super.meet(node);
			variables.add(node.getName());
		}

		private void addParameter(String name, Value value) throws MalformedQueryException {
			if (value.equals(parameters.getValue(name)))
				throw new MalformedQueryException("Multiple bindings for: " + name);
			if (name.indexOf('$') >= 0 || name.indexOf('?') >= 0 || name.indexOf('&') >= 0 || name.indexOf('=') >= 0)
				throw new MalformedQueryException("Invalide parameter name: " + name);
			try {
				if (!name.equals(URLEncoder.encode(name, "UTF-8")))
					throw new MalformedQueryException("Invalide parameter name: " + name);
			} catch (UnsupportedEncodingException e) {
				throw new AssertionError(e);
			}
			parameters.addBinding(name, value);
		}
	}

	public static ParameterizedQueryParser newInstance() {
		return new ParameterizedQueryParser();
	}

	private ParameterizedQueryParser() {
		super();
	}

	public ParameterizedQuery parseQuery(InputStream in, String systemId) throws IOException, MalformedQueryException {
		String sparql = readString(in);
		return parseQuery(sparql, systemId);
	}

	public ParameterizedQuery parseQuery(String sparql, String systemId)
			throws MalformedQueryException {
		BindingSet parameters = new ParameterScanner(systemId).scan(sparql);
		for (String name : parameters.getBindingNames()) {
			if (parameters.getValue(name) instanceof Literal) {
				String pattern = "\"\\$" + Pattern.quote(name) + "\"(^^<[^\\s>]*>|^^\\S*:\\S*\\b|@\\w+\\b)?";
				sparql = sparql.replaceAll(pattern, Matcher.quoteReplacement("$" + name));
			} else {
				String pattern = "<\\$" + Pattern.quote(name) + ">";
				sparql = sparql.replaceAll(pattern, Matcher.quoteReplacement("$" + name));
			}
		}
		return new ParameterizedQuery(sparql, systemId, parameters);
	}

	private String readString(InputStream in) throws IOException {
		try {
			Reader reader = new InputStreamReader(in, "UTF-8");
			StringWriter writer = new StringWriter(8192);
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
				if (writer.getBuffer().length() > 1048576)
					throw new IOException("Input stream is too big");
			}
			reader.close();
			return writer.toString();
		} finally {
			in.close();
		}
	}
}
