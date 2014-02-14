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
package org.callimachusproject.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.manager.LocalRepositoryManager;

public class DatasourceManager {
	private final Map<URI, CalliRepository> repositories = new HashMap<URI, CalliRepository>();
	private final LocalRepositoryManager manager;
	private final String repositoryId;
	private final String prefix;

	public DatasourceManager(LocalRepositoryManager manager, String repositoryId) {
		this(manager, repositoryId, repositoryId + "/datasources/");
	}

	public DatasourceManager(LocalRepositoryManager manager, String repositoryId, String prefix) {
		this.manager = manager;
		this.repositoryId = repositoryId;
		this.prefix = prefix;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public synchronized RepositoryImplConfig getDatasourceConfig(URI uri)
			throws OpenRDFException {
		String repositoryId = getRepositoryId(uri);
		RepositoryConfig config = manager.getRepositoryConfig(repositoryId);
		if (config == null)
			return null;
		return config.getRepositoryImplConfig();
	}

	public synchronized void setDatasourceConfig(URI uri,
			RepositoryImplConfig config) throws OpenRDFException, IOException {
		String repositoryId = getRepositoryId(uri);
		if (repositories.containsKey(uri)) {
			shutDownDatasource(uri);
		}
		manager.addRepositoryConfig(new RepositoryConfig(repositoryId, uri
				.stringValue(), config));
	}

	public boolean isDatasourcePresent(URI uri) throws OpenRDFException {
		return manager.hasRepositoryConfig(getRepositoryId(uri));
	}

	public synchronized CalliRepository getDatasource(URI uri)
			throws OpenRDFException, IOException {
		CalliRepository value = repositories.get(uri);
		if (value != null && value.isInitialized())
			return value;
		String repositoryId = getRepositoryId(uri);
		Repository repository = manager.getRepository(repositoryId);
		if (repository == null)
			throw new IllegalArgumentException("Datasource " + uri
					+ " is not configured correctly");
		File dataDir = manager.getRepositoryDir(repositoryId);
		value = createCalliRepository(uri, repository, dataDir);
		repositories.put(uri, value);
		return value;
	}

	public synchronized void shutDownDatasource(URI uri) throws OpenRDFException {
		CalliRepository repository = repositories.get(uri);
		if (repository != null && repository.isInitialized()) {
			repository.shutDown();
		}
		repositories.remove(uri);
	}

	public void purgeDatasource(URI uri) throws OpenRDFException {
		if (repositories.containsKey(uri)) {
			shutDownDatasource(uri);
		}
		manager.removeRepository(getRepositoryId(uri));
	}

	public synchronized void shutDown() throws OpenRDFException {
		for (URI uri : new ArrayList<URI>(repositories.keySet())) {
			shutDownDatasource(uri);
		}
	}

	protected String getRepositoryId(URI uri) {
		int hash = uri.stringValue().hashCode();
		String code = Integer.toHexString(Math.abs(hash));
		String local = uri.getLocalName().replaceAll("[^a-zA-Z0-9\\-.]", "_");
		return prefix + (local.toLowerCase() + "-" + code);
	}

	protected CalliRepository createCalliRepository(URI uri,
			Repository repository, File dataDir) throws OpenRDFException,
			IOException {
		return new CalliRepository(repository, dataDir);
	}
}
