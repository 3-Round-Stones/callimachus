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
package org.callimachusproject.repository.auditing.config;

import static org.callimachusproject.repository.auditing.config.AuditingSchema.ACTIVITY_NAMESPACE;
import static org.callimachusproject.repository.auditing.config.AuditingSchema.AUDIT_REMOVAL;
import static org.callimachusproject.repository.auditing.config.AuditingSchema.MAX_RECENT;
import static org.callimachusproject.repository.auditing.config.AuditingSchema.MIN_RECENT;
import static org.callimachusproject.repository.auditing.config.AuditingSchema.PURGE_AFTER;
import static org.callimachusproject.repository.auditing.config.AuditingSchema.TRANSACTIONAL;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.contextaware.config.ContextAwareConfig;

/**
 * Configuration bean for auditing repository that can serialize to/from turtle.
 */
public class AuditingRepositoryConfig extends ContextAwareConfig {

	private String ns;
	private int minRecent;
	private int maxRecent;
	private Duration purgeAfter;
	private Boolean transactional;
	private boolean auditingRemoval = true;

	public String getNamespace() {
		return ns;
	}

	public void setNamespace(String ns) {
		this.ns = ns;
	}

	public int getMinRecent() {
		return minRecent;
	}

	public void setMinRecent(int minRecent) {
		this.minRecent = minRecent;
	}

	public int getMaxRecent() {
		return maxRecent;
	}

	public void setMaxRecent(int maxRecent) {
		this.maxRecent = maxRecent;
	}

	public Duration getPurgeAfter() {
		return purgeAfter;
	}

	public void setPurgeAfter(Duration purgeAfter) {
		this.purgeAfter = purgeAfter;
	}

	public Boolean getTransactional() {
		return transactional;
	}

	public void setTransactional(Boolean transactional) {
		this.transactional = transactional;
	}

	public boolean isAuditingRemoval() {
		return auditingRemoval;
	}

	public void setAuditingRemoval(boolean auditingRemoval) {
		this.auditingRemoval = auditingRemoval;
	}

	@Override
	public Resource export(Graph model) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		Resource self = super.export(model);
		if (ns != null) {
			model.add(self, ACTIVITY_NAMESPACE, vf.createLiteral(ns));
		}
		model.add(self, MIN_RECENT, vf.createLiteral(minRecent));
		model.add(self, MAX_RECENT, vf.createLiteral(maxRecent));
		if (purgeAfter != null) {
			model.add(self, PURGE_AFTER,
					vf.createLiteral(purgeAfter.toString(), XMLSchema.DURATION));
		}
		if (transactional != null) {
			model.add(self, TRANSACTIONAL, vf.createLiteral(transactional));
		}
		model.add(self, AUDIT_REMOVAL, vf.createLiteral(auditingRemoval));
		return self;
	}

	@Override
	public void parse(Graph graph, Resource implNode)
			throws RepositoryConfigException {
		super.parse(graph, implNode);
		Model model = new LinkedHashModel(graph);
		setNamespace(model.filter(implNode, ACTIVITY_NAMESPACE, null).objectString());
		Literal lit = model.filter(implNode, MIN_RECENT, null).objectLiteral();
		if (lit != null) {
			setMinRecent(lit.intValue());
		}
		lit = model.filter(implNode, MAX_RECENT, null).objectLiteral();
		if (lit != null) {
			setMaxRecent(lit.intValue());
		}
		lit = model.filter(implNode, PURGE_AFTER, null).objectLiteral();
		if (lit != null) {
			try {
				DatatypeFactory df = DatatypeFactory.newInstance();
				setPurgeAfter(df.newDuration(lit.stringValue()));
			} catch (DatatypeConfigurationException e) {
				throw new RepositoryConfigException(e);
			}
		}
		lit = model.filter(implNode, TRANSACTIONAL, null).objectLiteral();
		if (lit != null) {
			setTransactional(lit.booleanValue());
		}
		lit = model.filter(implNode, AUDIT_REMOVAL, null).objectLiteral();
		if (lit != null) {
			setAuditingRemoval(lit.booleanValue());
		}
	}

}
