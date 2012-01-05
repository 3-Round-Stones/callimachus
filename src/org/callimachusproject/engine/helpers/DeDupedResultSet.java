/*
 * Portions Copyright (c) 2011 Talis Inc, Some Rights Reserved
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;


/**
 * Remove contiguous duplicates and empty results from a result set
 * Enable full de-dupe for full duplicate removal (more expensive)
 * 
 * @author Steve Battle
 */

public class DeDupedResultSet implements TupleQueryResult {
	TupleQueryResult results;
	BindingSet next;
	Set<BindingSet> seen;

	public DeDupedResultSet(TupleQueryResult results) 
	throws QueryEvaluationException {
		super();
		this.results = results;
		next = more();
	}

	public DeDupedResultSet(TupleQueryResult results, boolean full) 
	throws QueryEvaluationException {
		this(results);
		seen = new HashSet<BindingSet>();
	}

	@Override
	public void close() throws QueryEvaluationException {
		results.close();
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return next!=null;
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		BindingSet next = this.next;
		do {
			this.next = more();
		} while (this.next!=null 
		&& (this.next.equals(next) || this.next.size()==0 
		|| (seen!=null && seen.contains(this.next))));
		if (seen!=null) seen.add(next);
		return next;
	}
	
	private BindingSet more() throws QueryEvaluationException {
		if (results.hasNext())
			return results.next();
		else return null;
	}

	@Override
	public void remove() throws QueryEvaluationException {
	}

	@Override
	public List<String> getBindingNames() {
		return results.getBindingNames();
	}

}
