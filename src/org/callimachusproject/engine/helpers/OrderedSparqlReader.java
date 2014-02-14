/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.engine.helpers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.OrderBy;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.Var;
import org.callimachusproject.engine.model.VarOrTerm;

public class OrderedSparqlReader extends AbstractRDFEventReader {
	private final RDFEventReader delegate;
	private RDFEvent next;
	private List<List<Var>> parent_vars = new ArrayList<List<Var>>();
	private List<List<Var>> parent_nested = new ArrayList<List<Var>>();
	private List<Var> nested = new ArrayList<Var>();
	private List<Var> vars = new ArrayList<Var>();

	public OrderedSparqlReader(RDFEventReader delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() throws RDFParseException {
		delegate.close();
	}

	@Override
	protected RDFEvent take() throws RDFParseException {
		try {
			if (next != null)
				return next;
		} finally {
			next = null;
		}
		RDFEvent taken = delegate.next();
		if (taken == null)
			return taken;
		process(taken);
		return taken;
	}

	private void process(RDFEvent taken) {
		if (taken.isStartGroup() || taken.isStartWhere()) {
			parent_vars.add(vars);
			parent_nested.add(nested);
			vars = new ArrayList<Var>();
			nested = new ArrayList<Var>();
		} else if (taken.isEndGroup() || taken.isEndWhere()) {
			if (!nested.isEmpty()) {
				vars.addAll(nested);
			}
			if (parent_nested.isEmpty()) {
				nested = new ArrayList<Var>();
			} else {
				nested = parent_nested.remove(parent_nested.size() - 1);
			}
			nested.addAll(0, vars);
			if (parent_nested.isEmpty()) {
				vars = new ArrayList<Var>();
			} else {
				vars = parent_vars.remove(parent_vars.size() - 1);
			}
		} else if (taken.isTriplePattern()) {
			addIfVar(taken.asTriplePattern().getAbout());
			addIfVar(taken.asTriplePattern().getPartner());
		}
		if (taken.isEndWhere() && !nested.isEmpty()) {
			next = new OrderBy(new LinkedHashSet<Var>(nested), taken.getLocation());
		}
	}

	private void addIfVar(VarOrTerm subj) {
		if (subj.isVar()) {
			vars.add(subj.asVar());
		}
	}

}
