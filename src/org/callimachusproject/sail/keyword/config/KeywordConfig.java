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
package org.callimachusproject.sail.keyword.config;

import static org.callimachusproject.sail.keyword.config.KeywordSchema.ENABLED;
import static org.callimachusproject.sail.keyword.config.KeywordSchema.KEYWORD_PROPERTY;
import static org.callimachusproject.sail.keyword.config.KeywordSchema.PHONE_GRAPH;
import static org.callimachusproject.sail.keyword.config.KeywordSchema.PHONE_PROPERTY;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.config.DelegatingSailImplConfigBase;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailImplConfig;
import org.callimachusproject.sail.keyword.config.KeywordFactory;

/**
 * Keyword SAIL configuration bean
 */
public class KeywordConfig extends DelegatingSailImplConfigBase {
	private final ValueFactory vf = ValueFactoryImpl.getInstance();

	public KeywordConfig() {
		super(KeywordFactory.SAIL_TYPE);
	}

	public KeywordConfig(SailImplConfig delegate) {
		super(KeywordFactory.SAIL_TYPE, delegate);
	}

	private Boolean enabled;
	private Set<URI> keywordProperties;
	private URI phoneProperty;
	private URI phoneGraph;

	public boolean isEnabled() {
		return enabled == null ? true : enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<URI> getKeywordProperties() {
		return keywordProperties;
	}

	public void setKeywordProperties(Set<URI> keywordProperties) {
		this.keywordProperties = keywordProperties;
	}

	public URI getPhoneProperty() {
		return phoneProperty;
	}

	public void setPhoneProperty(URI phoneProperty) {
		this.phoneProperty = phoneProperty;
	}

	public URI getPhoneGraph() {
		return phoneGraph;
	}

	public void setPhoneGraph(URI phoneGraph) {
		this.phoneGraph = phoneGraph;
	}

	@Override
	public Resource export(Graph model) {
		Resource self = super.export(model);
		if (enabled != null) {
			model.add(self, ENABLED, vf.createLiteral(enabled));
		}
		if (keywordProperties != null && !keywordProperties.isEmpty()) {
			for (URI uri : keywordProperties) {
				model.add(self, KEYWORD_PROPERTY, uri);
			}
		}
		if (phoneProperty != null) {
			model.add(self, PHONE_PROPERTY, phoneProperty);
		}
		if (phoneGraph != null) {
			model.add(self, PHONE_GRAPH, phoneGraph);
		}
		return self;
	}

	@Override
	public void parse(Graph graph, Resource implNode)
			throws SailConfigException {
		super.parse(graph, implNode);
		Model model = new LinkedHashModel(graph);
		keywordProperties = new HashSet<URI>();
		for (Value uri : model.filter(implNode, KEYWORD_PROPERTY, null).objects()) {
			if (uri instanceof URI) {
				keywordProperties.add((URI) uri);
			}
		}
		setPhoneProperty(model.filter(implNode, PHONE_PROPERTY, null).objectURI());
		setPhoneGraph(model.filter(implNode, PHONE_GRAPH, null).objectURI());
		Set<Value> set = model.filter(implNode, ENABLED, null).objects();
		if (!set.isEmpty()) {
			Literal lit = (Literal) set.iterator().next();
			enabled = lit.booleanValue();
		}
	}

}
