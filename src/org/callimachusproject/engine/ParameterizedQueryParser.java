package org.callimachusproject.engine;

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Dataset;
import org.openrdf.query.IncompatibleOperationException;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.BindingSetAssignment;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.sparql.BaseDeclProcessor;
import org.openrdf.query.parser.sparql.BlankNodeVarProcessor;
import org.openrdf.query.parser.sparql.DatasetDeclProcessor;
import org.openrdf.query.parser.sparql.PrefixDeclProcessor;
import org.openrdf.query.parser.sparql.StringEscapesProcessor;
import org.openrdf.query.parser.sparql.TupleExprBuilder;
import org.openrdf.query.parser.sparql.WildcardProjectionProcessor;
import org.openrdf.query.parser.sparql.ast.ASTQuery;
import org.openrdf.query.parser.sparql.ast.ASTQueryContainer;
import org.openrdf.query.parser.sparql.ast.ASTSelectQuery;
import org.openrdf.query.parser.sparql.ast.Node;
import org.openrdf.query.parser.sparql.ast.ParseException;
import org.openrdf.query.parser.sparql.ast.SyntaxTreeBuilder;
import org.openrdf.query.parser.sparql.ast.TokenMgrError;
import org.openrdf.query.parser.sparql.ast.VisitorException;

public class ParameterizedQueryParser {

	private final class ParameterScanner extends
			QueryModelVisitorBase<MalformedQueryException> {
		private final Set<String> variables = new LinkedHashSet<String>();
		private final Map<String,Value> parameters = new LinkedHashMap<String,Value>();
		private Map<String, String> prefixes;
		private final String systemId;
		private final String space;

		ParameterScanner(String systemId) {
			this.systemId = systemId;
			space = new ParsedURI(systemId).resolve("$").toString();
		}

		public synchronized Map<String,Value> scan(String sparql) throws MalformedQueryException {
			variables.clear();
			parameters.clear();
			ParsedQuery parsed = parseParsedQuery(sparql.replaceAll("(?<!\\\\)\\$\\{[^}]*\\}", "0"), systemId);
			if (!(parsed instanceof ParsedTupleQuery))
				throw new MalformedQueryException("Only SELECT queries are supported");
			parsed.getTupleExpr().visit(this);
			visitExpressions(sparql);
			for (String varname : variables) {
				if (!parameters.containsKey(varname)) {
					String pattern = Matcher.quoteReplacement("$" + varname) + "\\b";
					if (Pattern.compile(pattern).matcher(sparql).find()) {
						parameters.put(varname, null);
					}
				}
			}
			return parameters;
		}

		public Map<String, String> getPrefixes() {
			return prefixes;
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

		private void visitExpressions(String sparql)
				throws MalformedQueryException {
			Matcher m = Pattern.compile("\\$\\{([^}]*)\\}").matcher(sparql);
			String prologue = getPrologue();
			while (m.find()) {
				String expression = m.group(1);
				String select = prologue + "SELECT (" + expression + " AS ?_value) {}";
				parseParsedQuery(select, systemId).getTupleExpr().visit(this);
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

		private void addParameter(String name, Value value) throws MalformedQueryException {
			if (parameters.containsKey(name) && !value.equals(parameters.get(name)))
				throw new MalformedQueryException("Multiple bindings for: " + name + " " + parameters.get(name) + " and " + value);
			if (name.indexOf('$') >= 0 || name.indexOf('?') >= 0 || name.indexOf('&') >= 0 || name.indexOf('=') >= 0)
				throw new MalformedQueryException("Invalide parameter name: " + name);
			try {
				if (!name.equals(URLEncoder.encode(name, "UTF-8")))
					throw new MalformedQueryException("Invalide parameter name: " + name);
			} catch (UnsupportedEncodingException e) {
				throw new AssertionError(e);
			}
			parameters.put(name, value);
		}

		private ParsedQuery parseParsedQuery(String queryStr, String baseURI)
			throws MalformedQueryException
		{
			try {
				ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(queryStr);
				StringEscapesProcessor.process(qc);
				BaseDeclProcessor.process(qc, baseURI);
				prefixes = PrefixDeclProcessor.process(qc);
				WildcardProjectionProcessor.process(qc);
				BlankNodeVarProcessor.process(qc);

				if (qc.containsQuery()) {

					// handle query operation

					TupleExpr tupleExpr = buildQueryModel(qc);

					ParsedQuery query;

					ASTQuery queryNode = qc.getQuery();
					if (queryNode instanceof ASTSelectQuery) {
						query = new ParsedTupleQuery(tupleExpr);
					}
					else {
						throw new MalformedQueryException("Unexpected query type: " + queryNode.getClass());
					}

					// Handle dataset declaration
					Dataset dataset = DatasetDeclProcessor.process(qc);
					if (dataset != null) {
						query.setDataset(dataset);
					}

					return query;
				}
				else {
					throw new IncompatibleOperationException("supplied string is not a query operation");
				}
			}
			catch (ParseException e) {
				throw new MalformedQueryException(e.getMessage(), e);
			}
			catch (TokenMgrError e) {
				throw new MalformedQueryException(e.getMessage(), e);
			}
		}

		private TupleExpr buildQueryModel(Node qc)
			throws MalformedQueryException
		{
			TupleExprBuilder tupleExprBuilder = new TupleExprBuilder(new ValueFactoryImpl());
			try {
				return (TupleExpr)qc.jjtAccept(tupleExprBuilder, null);
			}
			catch (VisitorException e) {
				throw new MalformedQueryException(e.getMessage(), e);
			}
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
		ParameterScanner scanner = new ParameterScanner(systemId);
		Map<String,Value> parameters = scanner.scan(sparql);
		Map<String, String> prefixes = scanner.getPrefixes();
		for (String name : parameters.keySet()) {
			if (parameters.get(name) instanceof Literal) {
				String pattern = "\"\\$" + Pattern.quote(name) + "\"(\\^\\^<[^\\s>]*>|\\^\\^\\S*:\\S*\\b|@\\w+\\b)?";
				sparql = sparql.replaceAll(pattern, Matcher.quoteReplacement("$" + name));
			} else {
				String pattern = "<\\$" + Pattern.quote(name) + ">";
				sparql = sparql.replaceAll(pattern, Matcher.quoteReplacement("$" + name));
			}
		}
		return new ParameterizedQuery(sparql, systemId, prefixes, parameters);
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
