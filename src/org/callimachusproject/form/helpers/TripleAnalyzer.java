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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
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
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.DeleteData;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.InsertData;
import org.openrdf.query.algebra.Modify;
import org.openrdf.query.algebra.MultiProjection;
import org.openrdf.query.algebra.Projection;
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
import org.openrdf.rio.RDFHandlerException;

public class TripleAnalyzer extends QueryModelVisitorBase<RDFHandlerException> {
	private final TripleVerifier verifier = new TripleVerifier();
	private final ValueFactory vf = new ValueFactoryImpl();
	private final Map<String, Resource> anonymous = new HashMap<String, Resource>();

	public String parseInsertData(InputStream in, String systemId)
			throws RDFHandlerException, IOException, MalformedQueryException {
		String input = readString(in);
		analyzeInsertData(input, systemId);
		return input;
	}

	public void analyzeInsertData(String input, String systemId)
			throws MalformedQueryException, RDFHandlerException {
		SPARQLParser parser = new SPARQLParser();
		ParsedUpdate parsed = parser.parseUpdate(input, systemId);
		if (parsed.getUpdateExprs().isEmpty())
			throw new RDFHandlerException("No input");
		for (UpdateExpr updateExpr : parsed.getUpdateExprs()) {
			if (updateExpr instanceof InsertData) {
				updateExpr.visit(this);
			} else {
				throw new RDFHandlerException(
						"Unsupported type of update statement: "
								+ updateExpr.getClass().getSimpleName());
			}
		}
	}

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
			if (updateExpr instanceof DeleteData
					|| updateExpr instanceof InsertData
					|| updateExpr instanceof Modify) {
				updateExpr.visit(this);
			} else {
				throw new RDFHandlerException(
						"Unsupported type of update statement: "
								+ updateExpr.getClass().getSimpleName());
			}
		}
	}

	public void accept(RDFEventReader reader) throws RDFParseException {
		verifier.accept(reader);
	}

	public void accept(TriplePattern pattern) {
		verifier.accept(pattern);
	}

	public boolean isDisconnectedNodesPresent() {
		return verifier.isDisconnectedNodesPresent();
	}

	public boolean isAbout(Resource about) {
		return verifier.isAbout(about);
	}

	public boolean isEmpty() {
		return verifier.isEmpty();
	}

	public boolean isSingleton() {
		return verifier.isSingleton();
	}

	public URI getSubject() {
		return verifier.getSubject();
	}

	public void addSubject(URI subj) throws RDFHandlerException {
		verifier.addSubject(subj);
	}

	public Set<URI> getAllTypes() {
		return verifier.getAllTypes();
	}

	public Set<URI> getTypes(URI subject) {
		return verifier.getTypes(subject);
	}

	public Set<URI> getResources() {
		return verifier.getResources();
	}

	@Override
	public void meet(StatementPattern node) throws RDFHandlerException {
		super.meet(node);
		Value subj = node.getSubjectVar().getValue();
		Value pred = node.getPredicateVar().getValue();
		Value obj = node.getObjectVar().getValue();
		Var ctxVar = node.getContextVar();
		Value ctx = ctxVar == null ? null : ctxVar.getValue();
		if (!(pred instanceof URI))
			throw new RDFHandlerException("Missing predicate: "
					+ node.getPredicateVar());
		if (subj == null && node.getSubjectVar().isAnonymous()) {
			subj = bind(node.getSubjectVar());
		}
		if (obj == null) {
			obj = bind(node.getObjectVar());
		}
		if (subj == null && !node.getSubjectVar().isAnonymous())
			throw new RDFHandlerException("Unknown subject: "
					+ node.getSubjectVar());
		if (ctx instanceof Resource) {
			throw new RDFHandlerException("Only the default graph can be used");
		} else if (ctx == null) {
			verifier.verify((Resource) subj, (URI) pred, obj);
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
				});
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
		try {
			Reader reader = new InputStreamReader(in, "UTF-8");
			StringWriter writer = new StringWriter(8192);
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
				if (writer.getBuffer().length() > 1048576)
					throw new IOException("Input Stream is too big");
			}
			reader.close();
			return writer.toString();
		} finally {
			in.close();
		}
	}

}
