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

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

public class StatementExtractor extends
		QueryModelVisitorBase<RDFHandlerException> {
	private final RDFHandler handler;
	private final ValueFactory vf;
	private final Map<String,Resource> anonymous = new HashMap<String,Resource>();

	public StatementExtractor(RDFHandler handler, ValueFactory vf) {
		this.handler = handler;
		this.vf = vf;
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
			throw new RDFHandlerException("Missing predicate");
		if (subj == null && node.getSubjectVar().isAnonymous()) {
			subj = bind(node.getSubjectVar());
		}
		if (obj == null && node.getObjectVar().isAnonymous()) {
			obj = bind(node.getObjectVar());
		}
		if (ctx instanceof Resource) {
			handler.handleStatement(new ContextStatementImpl(
					(Resource) subj, (URI) pred, obj, (Resource) ctx));
		} else if (ctx == null) {
			handler.handleStatement(new StatementImpl((Resource) subj,
					(URI) pred, obj));
		} else {
			throw new RDFHandlerException("Invalid graph: " + ctx);
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

}
