/*
 * Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.form.helpers;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.ConvertingIteration;
import info.aduna.iteration.EmptyIteration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.TriplePattern;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.DeleteData;
import org.openrdf.query.algebra.Distinct;
import org.openrdf.query.algebra.EmptySet;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.InsertData;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.Modify;
import org.openrdf.query.algebra.MultiProjection;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.Reduced;
import org.openrdf.query.algebra.SingletonSet;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.sail.helpers.SPARQLUpdateDataBlockParser;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.BasicParserSettings;

public class TripleAnalyzer extends QueryModelVisitorBase<RDFHandlerException> implements RDFHandler {
	private final List<TripleVerifier> verifiers = new ArrayList<TripleVerifier>();
	private final ValueFactory vf = new ValueFactoryImpl();
	private final Map<String, Resource> anonymous = new HashMap<String, Resource>();
	private final Set<Statement> connections = new HashSet<Statement>();
	private TripleVerifier deleteVerifier = new TripleVerifier();
	private TripleVerifier insertVerifier = new TripleVerifier();
	private TripleVerifier verifier = new TripleVerifier();
	private boolean complicated;
	private boolean modify;

	public String parseUpdate(InputStream in, String systemId)
			throws RDFHandlerException, IOException, MalformedQueryException {
		String input = readString(in);
		analyzeUpdate(input, systemId);
		return input;
	}

	public void analyzeUpdate(String input, String systemId)
			throws MalformedQueryException, RDFHandlerException {
		SPARQLParser parser = new SPARQLParser();
		ParsedUpdate parsed = parser.parseUpdate(input, systemId);
		if (parsed.getUpdateExprs().isEmpty())
			throw new RDFHandlerException("No input");
		for (UpdateExpr updateExpr : parsed.getUpdateExprs()) {
			if (updateExpr instanceof InsertData) {
				updateExpr.visit(this);
			} else if (updateExpr instanceof DeleteData
					|| updateExpr instanceof Modify) {
				modify = true;
				updateExpr.visit(this);
			} else {
				throw new RDFHandlerException(
						"Unsupported type of update statement: "
								+ updateExpr.getClass().getSimpleName());
			}
		}
	}

	public void acceptDelete(RDFEventReader reader) throws RDFParseException {
		deleteVerifier.accept(reader);
	}

	public void acceptDelete(GraphQueryResult result) throws RDFParseException,
			QueryEvaluationException {
		deleteVerifier.accept(result);
	}

	public void acceptDelete(TriplePattern pattern) {
		deleteVerifier.accept(pattern);
	}

	public void acceptInsert(RDFEventReader reader) throws RDFParseException {
		insertVerifier.accept(reader);
	}

	public void acceptInsert(GraphQueryResult result) throws RDFParseException,
			QueryEvaluationException {
		insertVerifier.accept(result);
	}

	public void acceptInsert(TriplePattern pattern) {
		insertVerifier.accept(pattern);
	}

	public void addSubject(URI subj) {
		verifier.addSubject(subj);
		deleteVerifier.addSubject(subj);
		insertVerifier.addSubject(subj);
	}

	public boolean isComplicated() {
		return complicated;
	}

	/**
	 * No delete operations are used.
	 */
	public boolean isInsertOnly() {
		return !modify;
	}

	public boolean isDisconnectedNodePresent() {
		for (TripleVerifier verifier : verifiers) {
			if (verifier.isDisconnectedNodePresent())
				return true;
		}
		return false;
	}

	public boolean isAbout(Resource about) {
		boolean ret = false;
		for (TripleVerifier verifier : verifiers) {
			if (verifier.isAbout(about)) {
				ret = true;
			} else if (!verifier.isEmpty()) {
				return false;
			}
		}
		return ret;
	}

	public boolean isEmpty() {
		for (TripleVerifier verifier : verifiers) {
			if (!verifier.isEmpty())
				return false;
		}
		return true;
	}

	public boolean isSingleton() {
		boolean ret = false;
		for (TripleVerifier verifier : verifiers) {
			if (verifier.isSingleton()) {
				ret = true;
			} else if (!verifier.isEmpty()) {
				return false;
			}
		}
		return ret;
	}

	public boolean isContainmentTriplePresent() {
		boolean ret = false;
		for (TripleVerifier verifier : verifiers) {
			if (verifier.isContainmentTriplePresent()) {
				ret = true;
			}
		}
		return ret;
	}

	public URI getSubject() {
		for (TripleVerifier verifier : verifiers) {
			URI subj = verifier.getSubject();
			if (subj != null)
				return subj;
		}
		return null;
	}

	public Set<URI> getTypes(URI subject) {
		Set<URI> set = new LinkedHashSet<URI>();
		for (TripleVerifier verifier : verifiers) {
			set.addAll(verifier.getTypes(subject));
		}
		return set;
	}

	public Set<URI> getPartners() {
		Set<URI> set = new LinkedHashSet<URI>();
		for (TripleVerifier verifier : verifiers) {
			set.addAll(verifier.getPartners());
		}
		return set;
	}

	public Set<Statement> getConnections() {
		return connections;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		// ignore
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		// ignore
	}

	@Override
	public void handleNamespace(String prefix, String uri)
			throws RDFHandlerException {
		// ignore
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		// ignore
	}

	@Override
	public void meet(DeleteData node) throws RDFHandlerException {
		TripleVerifier previous = verifier;
		verifier = deleteVerifier.clone();
		SPARQLUpdateDataBlockParser parser = new SPARQLUpdateDataBlockParser(vf);
		parser.setAllowBlankNodes(false); // no blank nodes allowed in DELETE DATA.
		parser.setRDFHandler(this);
		try {
			parser.parse(new ByteArrayInputStream(node.getDataBlock().getBytes()), "");
		} catch (org.openrdf.rio.RDFParseException e) {
			throw new RDFHandlerException(e);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
		verifiers.add(verifier);
		verifier = previous;
	}

	@Override
	public void meet(InsertData node) throws RDFHandlerException {
		TripleVerifier previous = verifier;
		verifier = insertVerifier.clone();
		SPARQLUpdateDataBlockParser parser = new SPARQLUpdateDataBlockParser(vf);
		parser.setRDFHandler(this);
		parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		parser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
		try {
			parser.parse(new ByteArrayInputStream(node.getDataBlock().getBytes()), "");
		} catch (org.openrdf.rio.RDFParseException e) {
			throw new RDFHandlerException(e);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
		verifiers.add(verifier);
		verifier = previous;
	}

	@Override
	public void meet(Modify node) throws RDFHandlerException {
		TripleVerifier previous = verifier;
		if (node.getDeleteExpr() != null) {
			verifier = deleteVerifier.clone();
			try {
				node.getDeleteExpr().visit(this);
			} catch (RDFHandlerException e) {
				throw new RDFHandlerException(e.getMessage() + " in DELETE clause", e);
			}
			verifiers.add(verifier);
		}
		if (node.getInsertExpr() != null) {
			verifier = insertVerifier.clone();
			try {
				node.getInsertExpr().visit(this);
			} catch (RDFHandlerException e) {
				throw new RDFHandlerException(e.getMessage() + " in INSERT clause", e);
			}
			verifiers.add(verifier);
		}
		if (node.getWhereExpr() != null) {
			verifier = deleteVerifier.clone();
			try {
				node.getWhereExpr().visit(this);
			} catch (RDFHandlerException e) {
				throw new RDFHandlerException(e.getMessage() + " in WHERE clause", e);
			}
			verifiers.add(verifier);
			connections.addAll(verifier.getConnections());
		}
		verifier = previous;
	}

	@Override
	public void meet(StatementPattern node) throws RDFHandlerException {
		Value subj = node.getSubjectVar().getValue();
		Value pred = node.getPredicateVar().getValue();
		Value obj = node.getObjectVar().getValue();
		Var ctxVar = node.getContextVar();
		Value ctx = ctxVar == null ? null : ctxVar.getValue();
		if (!(pred instanceof URI))
			throw new RDFHandlerException("Missing predicate: "
					+ node.getPredicateVar());
		if (subj == null) {
			subj = bind(node.getSubjectVar());
		}
		if (obj == null) {
			obj = bind(node.getObjectVar());
		}
		if (ctx instanceof Resource) {
			throw new RDFHandlerException("Only the default graph can be used");
		} else if (ctx == null) {
			verifier.verify((Resource) subj, (URI) pred, obj);
		} else {
			throw new RDFHandlerException("Invalid graph: " + ctx);
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		Resource ctx = st.getContext();
		if (ctx instanceof Resource) {
			throw new RDFHandlerException("Only the default graph can be used");
		} else if (ctx == null) {
			verifier.verify(st.getSubject(), st.getPredicate(), st.getObject());
		} else {
			throw new RDFHandlerException("Invalid graph: " + ctx);
		}
	}

	@Override
	public void meet(Projection node) throws RDFHandlerException {
		TupleExpr arg = node.getArg();
		if (arg instanceof Extension) {
			Extension extension = (Extension) arg;
			TupleExpr arg2 = extension.getArg();
			if (arg2 instanceof SingletonSet) {
				evaluate(node);
			}
		}
	}

	@Override
	public void meet(MultiProjection node) throws RDFHandlerException {
		TupleExpr arg = node.getArg();
		if (arg instanceof Extension) {
			Extension extension = (Extension) arg;
			TupleExpr arg2 = extension.getArg();
			if (arg2 instanceof SingletonSet) {
				evaluate(node);
			}
		}
	}

	@Override
	public void meet(Join node) throws RDFHandlerException {
		node.getLeftArg().visit(this);
		node.getRightArg().visit(this);
	}

	@Override
	public void meet(Distinct node) throws RDFHandlerException {
		node.getArg().visit(this);
	}

	@Override
	public void meet(Reduced node) throws RDFHandlerException {
		node.getArg().visit(this);
	}

	@Override
	public void meet(SingletonSet node) throws RDFHandlerException {
		// not complicated
	}

	@Override
	public void meet(EmptySet node) throws RDFHandlerException {
		// not complicated
	}

	@Override
	protected void meetNode(QueryModelNode node) throws RDFHandlerException {
		complicated = true;
		super.meetNode(node);
	}

	private void evaluate(TupleExpr node) throws RDFHandlerException {
		EvaluationStrategyImpl strategy = new EvaluationStrategyImpl(
				new TripleSource() {
					public ValueFactory getValueFactory() {
						return vf;
					}

					public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(
							Resource subj, URI pred, Value obj,
							Resource... contexts)
							throws QueryEvaluationException {
						return new EmptyIteration<Statement, QueryEvaluationException>();
					}
				}, null);
		try {
			CloseableIteration<BindingSet, QueryEvaluationException> bindingsIter;
			bindingsIter = strategy.evaluate(new Reduced(node.clone()),
					new EmptyBindingSet());
			CloseableIteration<Statement, QueryEvaluationException> stIter;
			stIter = new ConvertingIteration<BindingSet, Statement, QueryEvaluationException>(
					bindingsIter) {

				@Override
				protected Statement convert(BindingSet bindingSet) {
					Resource subject = (Resource) bindingSet
							.getValue("subject");
					URI predicate = (URI) bindingSet.getValue("predicate");
					Value object = bindingSet.getValue("object");
					Resource context = (Resource) bindingSet
							.getValue("context");

					if (context == null) {
						return vf.createStatement(subject, predicate, object);
					} else {
						return vf.createStatement(subject, predicate, object,
								context);
					}
				}
			};
			while (stIter.hasNext()) {
				Statement st = stIter.next();
				if (st.getContext() != null)
					throw new RDFHandlerException(
							"Only the default graph can be used");
				verifier.verify(st.getSubject(), st.getPredicate(),
						st.getObject());
			}
		} catch (QueryEvaluationException e) {
			throw new RDFHandlerException(e);
		}
	}

	private synchronized Resource bind(Var var) {
		String name = var.getName();
		if (anonymous.containsKey(name))
			return anonymous.get(name);
		Resource node = vf.createBNode();
		anonymous.put(name, node);
		return node;
	}

	private String readString(InputStream in) throws IOException {
		Reader reader = new InputStreamReader(in, "UTF-8");
		try {
			StringWriter writer = new StringWriter(8192);
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
				if (writer.getBuffer().length() > 1048576)
					throw new IOException("Input Stream is too big");
			}
			return writer.toString();
		} finally {
			reader.close();
			in.close();
		}
	}

}
