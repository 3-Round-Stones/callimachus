/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
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
package org.callimachusproject.server;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.server.util.FileUtil;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.openrdf.OpenRDFException;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallimachusServer implements HTTPObjectAgentMXBean {
	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final String ENVELOPE_TYPE = "message/x-response";
	private static final String IDENTITY_PATH = "/diverted;";
	Logger logger = LoggerFactory.getLogger(CallimachusServer.class);
	private final Set<String> origins = new HashSet<String>();
	private CallimachusRepository repository;
	private HTTPObjectServer server;

	public CallimachusServer(Repository repository, File dataDir) throws Exception {
		this.repository = new CallimachusRepository(repository, dataDir);
		this.server = createServer(dataDir, this.repository);
	}

	public void addOrigin(String origin) throws Exception {
		ValueFactory vf = this.repository.getValueFactory();
		String schema = repository.getCallimachusUrl(origin, SCHEMA_GRAPH);
		if (schema != null) {
			repository.addSchemaGraphType(vf.createURI(schema));
		}
		origins.add(origin);
		String[] identities = origins.toArray(new String[origins.size()]);
		for (int i = 0; i < identities.length; i++) {
			URI uri = new URI(identities[i] + "/");
			String sch = uri.getScheme();
			String auth = uri.getAuthority();
			identities[i] = new URI(sch, auth, IDENTITY_PATH, null, null).toASCIIString();
		}
		server.setIdentityPrefix(identities);
	}

	public String getServerName() {
		return server.getName();
	}

	public void setServerName(String serverName) {
		server.setName(serverName);
	}

	public void setChangesPath(String origin, String path) throws OpenRDFException {
		String changes = repository.getCallimachusUrl(origin, path);
		if (changes == null) {
			logger.error("Callimachus webapp not installed in {}", origin);
		} else {
			repository.setChangeFolder(changes);
		}
	}

	public String getErrorPipe() {
		return server.getErrorPipe();
	}

	public void setErrorPipe(String origin, String path) throws IOException,
			OpenRDFException {
		String pipe = repository.getCallimachusUrl(origin, path);
		if (pipe == null)
			throw new IllegalArgumentException(
					"Callimachus webapp not setup on: " + origin);
		server.setErrorPipe(pipe);
	}

	public CallimachusRepository getRepository() {
		return repository;
	}

	public int getCacheSize() {
		return server.getCacheSize();
	}

	public int getCacheCapacity() {
		return server.getCacheCapacity();
	}

	public void setCacheCapacity(int capacity) {
		server.setCacheCapacity(capacity);
	}

	public String getFrom() {
		return server.getFrom();
	}

	public void setFrom(String from) {
		server.setFrom(from);
	}

	public String getName() {
		return server.getName();
	}

	public void setName(String serverName) {
		server.setName(serverName);
	}

	public boolean isCacheAggressive() {
		return server.isCacheAggressive();
	}

	public void setCacheAggressive(boolean cacheAggressive) {
		server.setCacheAggressive(cacheAggressive);
	}

	public boolean isCacheDisconnected() {
		return server.isCacheDisconnected();
	}

	public void setCacheDisconnected(boolean cacheDisconnected) {
		server.setCacheDisconnected(cacheDisconnected);
	}

	public boolean isCacheEnabled() {
		return server.isCacheEnabled();
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		server.setCacheEnabled(cacheEnabled);
	}

	public void invalidateCache() throws IOException, InterruptedException {
		server.invalidateCache();
	}

	public void resetCache() throws IOException, InterruptedException {
		server.resetCache();
	}

	public ConnectionBean[] getConnections() {
		return server.getConnections();
	}

	public void connectionDumpToFile(String outputFile) throws IOException {
		server.connectionDumpToFile(outputFile);
	}

	public void resetConnections() throws IOException {
		server.resetConnections();
	}

	public void poke() {
		server.poke();
	}

	public void listen(int[] ports, int[] sslports) throws Exception {
		assert ports != null && ports.length > 0 || sslports != null
				&& sslports.length > 0;
		if (ports == null) {
			ports = new int[0];
		} else if (sslports == null) {
			sslports = new int[0];
		}
		if (origins.isEmpty() && ports.length > 0) {
			addOrigin("http://" + getAuthority(ports[0]));
		} else if (origins.isEmpty() && sslports.length > 0) {
			addOrigin("https://" + getAuthority(sslports[0]));
		}
		server.listen(ports, sslports);
	}

	public void start() throws Exception {
		logger.info("Callimachus is binding to {}", toString());
		for (String origin : origins) {
			HttpHost host = getAuthorityAddress(origin);
			HTTPObjectClient.getInstance().setProxy(host, server);
		}
		repository.setCompileRepository(true);
		server.start();
		System.gc();
		Thread.yield();
		long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
		logger.info("Callimachus started in {} seconds", uptime / 1000.0);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String origin : origins) {
			sb.append(origin).append(" ");
		}
		if (sb.length() == 0)
			return super.toString();
		return sb.substring(0, sb.length() - 1);
	}

	public String getStatus() {
		return server.getStatus();
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public void stop() throws Exception {
		for (String origin : origins) {
			HttpHost host = getAuthorityAddress(origin);
			HTTPObjectClient.getInstance().removeProxy(host, server);
		}
		server.stop();
	}

	public void destroy() throws Exception {
		server.destroy();
	}

	private HttpHost getAuthorityAddress(String origin) {
		return URIUtils.extractHost(java.net.URI.create(origin + "/"));
	}

	private String getAuthority(int port) throws IOException {
		String hostname = DomainNameSystemResolver.getInstance().getCanonicalLocalHostName();
		if (port == 80 || port == 443)
			return hostname;
		return hostname + ":" + port;
	}

	private HTTPObjectServer createServer(File dir, CallimachusRepository or)
			throws IOException, NoSuchAlgorithmException {
		File cacheDir = new File(dir, "cache");
		FileUtil.deleteOnExit(cacheDir);
		File out = new File(cacheDir, "server");
		HTTPObjectServer server = new HTTPObjectServer(or, out);
		server.setEnvelopeType(ENVELOPE_TYPE);
		return server;
	}

}
