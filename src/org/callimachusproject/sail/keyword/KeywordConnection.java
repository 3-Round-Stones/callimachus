/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
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
package org.callimachusproject.sail.keyword;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.UpdateContext;
import org.openrdf.sail.helpers.SailConnectionWrapper;
import org.openrdf.sail.inferencer.InferencerConnection;

/**
 * Inferres keyword:phone property from properties in
 * META-INF/org.callimachusproject.sail.keyword.property.
 * 
 * @author James Leigh
 * 
 */
public class KeywordConnection extends SailConnectionWrapper {
	private final PhoneHelper helper;
	private final KeywordSail sail;
	private final ValueFactory vf;
	private final Resource graph;
	private final URI property;
	private final InferencerConnection infer;

	protected KeywordConnection(KeywordSail sail,
			SailConnection delegate, PhoneHelper keyword)
			throws SailException {
		super(delegate);
		this.sail = sail;
		this.helper = keyword;
		this.vf = sail.getValueFactory();
		this.graph = sail.getPhoneGraph();
		this.property = sail.getPhoneProperty();
		if (delegate instanceof InferencerConnection) {
			infer = (InferencerConnection) delegate;
		} else {
			infer = null;
		}
	}

	@Override
	public String toString() {
		return getWrappedConnection().toString();
	}

	@Override
	public void addStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		super.addStatement(subj, pred, obj, contexts);
		if (sail.isIndexedProperty(pred)) {
			index(null, subj, obj);
		}
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, URI pred,
			Value obj, Resource... contexts) throws SailException {
		super.addStatement(modify, subj, pred, obj, contexts);
		if (sail.isIndexedProperty(pred)) {
			index(modify, subj, obj);
		}
	}

	protected void index(UpdateContext uc, Resource subj, Value obj) throws SailException {
		for (String s : helper.phones(obj.stringValue())) {
			Literal lit = vf.createLiteral(s);
			if (infer == null && uc == null) {
				super.addStatement(subj, property, lit, graph);
			} else if (infer == null) {
				super.addStatement(uc, subj, property, lit, graph);
			} else {
				infer.addInferredStatement(subj, property, lit, graph);
			}
		}
	}

}
