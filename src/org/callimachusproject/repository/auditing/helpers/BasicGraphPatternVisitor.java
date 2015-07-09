/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.repository.auditing.helpers;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.ConvertingIteration;
import info.aduna.iteration.EmptyIteration;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.DeleteData;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.InsertData;
import org.openrdf.query.algebra.MultiProjection;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.Reduced;
import org.openrdf.query.algebra.SingletonSet;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.repository.sail.helpers.SPARQLUpdateDataBlockParser;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.BasicParserSettings;

/**
 * Walks UPDATE operations, like DELETE DATA, INSERT DATA, and DELETE/INSERT/WHERE,
 * and calls meet(StatementPattern) for triple in a DATA clause.
 */
public abstract class BasicGraphPatternVisitor extends
		QueryModelVisitorBase<QueryEvaluationException> implements RDFHandler {
	final ValueFactory vf = ValueFactoryImpl.getInstance();

	@Override
	public abstract void meet(StatementPattern node)
			throws QueryEvaluationException;

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
	public void handleStatement(Statement st) throws RDFHandlerException {
		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource ctx = st.getContext();
		try {
			if (ctx == null) {
				meet(new StatementPattern(new Var("", subj), new Var("",
						pred), new Var("", obj)));
			} else {
				meet(new StatementPattern(new Var("", subj), new Var("",
						pred), new Var("", obj), new Var("", ctx)));
			}
		} catch (QueryEvaluationException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void meet(Projection node) throws QueryEvaluationException {
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
	public void meet(MultiProjection node) throws QueryEvaluationException {
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
	public void meet(DeleteData node) throws QueryEvaluationException {
		SPARQLUpdateDataBlockParser parser = new SPARQLUpdateDataBlockParser(vf);
		parser.setAllowBlankNodes(false); // no blank nodes allowed in DELETE DATA.
		parser.setRDFHandler(this);
		try {
			parser.parse(new ByteArrayInputStream(node.getDataBlock().getBytes()), "");
		} catch (org.openrdf.rio.RDFParseException e) {
			throw new QueryEvaluationException(e);
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		} catch (RDFHandlerException e) {
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	public void meet(InsertData node) throws QueryEvaluationException {
		SPARQLUpdateDataBlockParser parser = new SPARQLUpdateDataBlockParser(vf);
		parser.setRDFHandler(this);
		parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		parser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
		try {
			parser.parse(new ByteArrayInputStream(node.getDataBlock().getBytes()), "");
		} catch (org.openrdf.rio.RDFParseException e) {
			throw new QueryEvaluationException(e);
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		} catch (RDFHandlerException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private void evaluate(TupleExpr node) throws QueryEvaluationException {
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
		CloseableIteration<BindingSet, QueryEvaluationException> bindingsIter;
		bindingsIter = strategy.evaluate(new Reduced(node.clone()),
				new EmptyBindingSet());
		CloseableIteration<Statement, QueryEvaluationException> stIter;
		stIter = new ConvertingIteration<BindingSet, Statement, QueryEvaluationException>(
				bindingsIter) {

			@Override
			protected Statement convert(BindingSet bindingSet) {
				Resource subject = (Resource) bindingSet.getValue("subject");
				URI predicate = (URI) bindingSet.getValue("predicate");
				Value object = bindingSet.getValue("object");
				Resource context = (Resource) bindingSet.getValue("context");

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
			Var s = new Var("subject", st.getSubject());
			Var p = new Var("predicate", st.getPredicate());
			Var o = new Var("object", st.getObject());
			Var c = null;
			if (st.getContext() != null) {
				c = new Var("context", st.getContext());
			}
			meet(new StatementPattern(s, p, o, c));
		}
	}
}
