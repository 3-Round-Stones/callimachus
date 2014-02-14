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

import info.aduna.iteration.CloseableIteration;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.utils.URIUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.Service;
import org.openrdf.query.algebra.evaluation.federation.FederatedService;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceManager;
import org.openrdf.repository.RepositoryException;

public class SparqlServiceCredentialManager extends FederatedServiceManager {
	private final Map<String, CalliRepository> repositories = new LinkedHashMap<String, CalliRepository>();
	private final Map<String, SoftReference<SPARQLCredentialService>> services = new HashMap<String, SoftReference<SPARQLCredentialService>>();
	private final Map<Credentials, Map<String, SoftReference<SPARQLCredentialService>>> protectedServices = new HashMap<Credentials, Map<String, SoftReference<SPARQLCredentialService>>>();
	private final Map<CalliRepository, Set<Credentials>> credentials = new WeakHashMap<CalliRepository, Set<Credentials>>();

	public synchronized void addOrigin(String origin, CalliRepository repository) {
		repositories.put(origin, repository);
		credentials.put(repository, new HashSet<Credentials>());
	}

	public synchronized void removeOrigin(String origin) {
		CalliRepository repository = repositories.remove(origin);
		Set<Credentials> set = credentials.get(repository);
		if (set != null) {
			for (Credentials credential : set) {
				protectedServices.remove(credential);
			}
		}
		credentials.remove(repository);
	}

	public synchronized void registerService(String serviceUrl,
			FederatedService service) {
		throw new UnsupportedOperationException();
	}

	public synchronized void unregisterService(String serviceUrl) {
		services.remove(serviceUrl);
	}

	public synchronized void unregisterAll() {
		super.unregisterAll();
		services.clear();
		protectedServices.clear();
		credentials.clear();
	}

	public synchronized FederatedService getService(final String serviceUrl)
			throws RepositoryException {
		return new FederatedService() {
			public void shutdown() throws RepositoryException {
				// no-op
			}

			public void initialize() throws RepositoryException {
				// no-op
			}

			public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
					String rq, BindingSet bindings, String base,
					QueryType type, Service service)
					throws QueryEvaluationException {
				return getSPARQLCredentialService(serviceUrl, base).evaluate(
						rq, bindings, base, type, service);
			}

			public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
					Service service,
					CloseableIteration<BindingSet, QueryEvaluationException> bindings,
					String baseUri) throws QueryEvaluationException {
				return getSPARQLCredentialService(serviceUrl, baseUri)
						.evaluate(service, bindings, baseUri);
			}
		};
	}

	synchronized SPARQLCredentialService getSPARQLCredentialService(
			String serviceUrl, String base) throws QueryEvaluationException {
		try {
			Credentials credential = getCredentials(serviceUrl, base);
			SPARQLCredentialService service = getExistingService(credential,
					serviceUrl);
			if (service == null) {
				service = createSPARQLService(credential, serviceUrl);
				service.initialize();
				putService(credential, serviceUrl, service);
			}
			return service;
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		} catch (OpenRDFException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private synchronized Credentials getCredentials(String serviceUrl,
			String base) throws OpenRDFException, IOException {
		String origin = getOrigin(base);
		CalliRepository repository = repositories.get(origin);
		if (repository == null)
			return null;
		CredentialsProvider provider = repository.getRealm(base)
				.getCredentialsProvider();
		if (provider == null)
			return null;
		HttpHost authority = URIUtils.extractHost(URI.create(serviceUrl));
		Credentials credential = provider.getCredentials(new AuthScope(
				authority));
		if (credential != null) {
			credentials.get(repository).add(credential);
		}
		return credential;
	}

	private String getOrigin(String source) {
		assert source != null;
		int scheme = source.indexOf("://");
		if (scheme < 0
				&& (source.startsWith("file:") || source
						.startsWith("jar:file:"))) {
			return "file://";
		} else {
			if (scheme < 0)
				throw new IllegalArgumentException(
						"Not an absolute hierarchical URI: " + source);
			int path = source.indexOf('/', scheme + 3);
			if (path >= 0) {
				return source.substring(0, path);
			} else {
				return source;
			}
		}
	}

	private synchronized SPARQLCredentialService getExistingService(
			Credentials credential, String serviceUrl) {
		if (credential == null) {
			SoftReference<SPARQLCredentialService> ref = services
					.get(serviceUrl);
			if (ref == null)
				return null;
			return ref.get();
		} else {
			Map<String, SoftReference<SPARQLCredentialService>> map = protectedServices
					.get(credential);
			if (map == null)
				return null;
			SoftReference<SPARQLCredentialService> ref = map.get(serviceUrl);
			if (ref == null)
				return null;
			return ref.get();
		}
	}

	private synchronized void putService(Credentials credential,
			String serviceUrl, SPARQLCredentialService service) {
		if (credential == null) {
			services.put(serviceUrl,
					new SoftReference<SPARQLCredentialService>(service));
		} else {
			Map<String, SoftReference<SPARQLCredentialService>> map = protectedServices
					.get(credential);
			if (map == null) {
				map = new HashMap<String, SoftReference<SPARQLCredentialService>>();
				protectedServices.put(credential, map);
			}
			map.put(serviceUrl, new SoftReference<SPARQLCredentialService>(
					service));
		}
	}

	private SPARQLCredentialService createSPARQLService(Credentials credential,
			String serviceUrl) {
		if (credential == null) {
			return new SPARQLCredentialService(serviceUrl);
		} else {
			String username = credential.getUserPrincipal().getName();
			String password = credential.getPassword();
			return new SPARQLCredentialService(serviceUrl, username, password);
		}
	}
}
