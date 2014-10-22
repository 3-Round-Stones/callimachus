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

import org.callimachusproject.repository.auditing.AuditingRepository;
import org.callimachusproject.repository.auditing.helpers.ActivitySequenceFactory;
import org.callimachusproject.repository.auditing.helpers.ActivityTagFactory;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.contextaware.config.ContextAwareFactory;

public class AuditingRepositoryFactory extends ContextAwareFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see RepositoryFactory#getRepositoryType()
	 */
	public static final String REPOSITORY_TYPE = "openrdf:AuditingRepository";

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	/**
	 * Creates a new AuditingRepositoryConfig instance.
	 */
	@Override
	public AuditingRepositoryConfig getConfig() {
		return new AuditingRepositoryConfig();
	}

	/**
	 * Create an uninitialised AuditingRepository without a delegate.
	 */
	@Override
	public AuditingRepository getRepository(RepositoryImplConfig configuration)
			throws RepositoryConfigException {
		if (configuration instanceof AuditingRepositoryConfig) {
			AuditingRepositoryConfig config = (AuditingRepositoryConfig) configuration;

			AuditingRepository repo = getAuditingRepository(config);

			repo.setIncludeInferred(config.isIncludeInferred());
			repo.setMaxQueryTime(config.getMaxQueryTime());
			repo.setQueryLanguage(config.getQueryLanguage());
			repo.setReadContexts(config.getReadContexts());
			repo.setAddContexts(config.getAddContexts());
			repo.setInsertContext(config.getInsertContext());
			repo.setRemoveContexts(config.getRemoveContexts());
			repo.setArchiveContexts(config.getArchiveContexts());
			// repo.setQueryResultLimit(config.getQueryResultLimit());
			return repo;
		}

		throw new RepositoryConfigException("Invalid configuration class: "
				+ configuration.getClass());
	}

	protected AuditingRepository getAuditingRepository(
			AuditingRepositoryConfig config) {
		AuditingRepository repo = new AuditingRepository();
		String ns = config.getNamespace();
		if (ns == null || ns.length() <= 0) {
			repo.setActivityFactory(new ActivityTagFactory());
		} else {
			repo.setActivityFactory(new ActivitySequenceFactory(ns));
		}
		repo.setMinRecent(config.getMinRecent());
		repo.setMaxRecent(config.getMaxRecent());
		repo.setPurgeAfter(config.getPurgeAfter());
		repo.setTransactional(config.getTransactional());
		repo.setAuditingRemoval(config.isAuditingRemoval());
		return repo;
	}

}
