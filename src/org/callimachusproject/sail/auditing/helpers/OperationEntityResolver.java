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
package org.callimachusproject.sail.auditing.helpers;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;

/**
 * Determines if an operation modifies a single entity.
 */
public class OperationEntityResolver {
	private final ValueFactory vf;
	private QueryModelNode node;
	private Dataset dataset;
	private BindingSet bindings;
	private URI entity;

	public OperationEntityResolver(ValueFactory vf) {
		this.vf = vf;
	}

	public synchronized URI getEntity(QueryModelNode node, Dataset dataset, BindingSet bindings) {
		if (dataset == null || node == null)
			return null;
		URI activity = dataset.getDefaultInsertGraph();
		if (activity == null || activity.stringValue().indexOf('#') >= 0)
			return null;
		if (this.node == node && this.dataset == dataset && this.bindings == bindings)
			return this.entity;
		final Set<Var> subjects = new HashSet<Var>();
		final Set<Var> objects = new HashSet<Var>();
		try {
			node.visit(new BasicGraphPatternVisitor() {
				public void meet(StatementPattern node) {
					subjects.add(node.getSubjectVar());
					objects.add(node.getObjectVar());
				}
			});
		} catch (QueryEvaluationException e) {
			throw new AssertionError(e);
		}
		URI entity = null;
		for (Var var : subjects) {
			Value subj = var.getValue();
			if (subj == null) {
				subj = bindings.getValue(var.getName());
			}
			if (subj instanceof URI) {
				if (entity == null) {
					entity = entity((URI) subj);
				} else if (!entity.equals(entity((URI) subj))) {
					return null;
				}
			} else if (!objects.contains(var)) {
				return null;
			}
		}
		this.node = node;
		this.dataset = dataset;
		this.bindings = bindings;
		this.entity = entity;
		return entity;
	}

	private URI entity(URI subject) {
		URI entity = subject;
		int hash = entity.stringValue().indexOf('#');
		if (hash > 0) {
			entity = vf.createURI(entity.stringValue().substring(0, hash));
		}
		return entity;
	}
}
