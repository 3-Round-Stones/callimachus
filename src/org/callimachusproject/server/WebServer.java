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
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer implements HTTPObjectAgentMXBean {
	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final String ENVELOPE_TYPE = "message/x-response";
	private static final String IDENTITY_PATH = "/diverted;";
	Logger logger = LoggerFactory.getLogger(WebServer.class);
	private final Map<String, CalliRepository> repositories = new LinkedHashMap<String, CalliRepository>();
	private HTTPObjectServer server;

	public WebServer(File tmpDir)
			throws OpenRDFException, IOException, NoSuchAlgorithmException {
		this.server = createServer(tmpDir);
	}

	public void addOrigin(String origin, CalliRepository repository) throws OpenRDFException, IOException {
		String schema = repository.getCallimachusUrl(origin, SCHEMA_GRAPH);
		if (schema != null) {
			repository.addSchemaGraphType(schema);
		}
		repositories.put(origin, repository);
		String[] identities = repositories.keySet().toArray(new String[repositories.size()]);
		for (int i = 0; i < identities.length; i++) {
			URI uri = URI.create(identities[i] + "/");
			String sch = uri.getScheme();
			String auth = uri.getAuthority();
			try {
				identities[i] = new URI(sch, auth, IDENTITY_PATH, null, null).toASCIIString();
			} catch (URISyntaxException x) {
	            IllegalArgumentException y = new IllegalArgumentException();
	            y.initCause(x);
	            throw y;
	        }
		}
		server.addOrigin(origin, repository);
		server.setIdentityPrefix(identities);
	}

	public String getServerName() {
		return server.getName();
	}

	public void setServerName(String serverName) {
		server.setName(serverName);
	}

	public String getErrorPipe() {
		return server.getErrorPipe();
	}

	public void setErrorPipe(String origin, String path) throws IOException,
			OpenRDFException {
		CalliRepository repository = repositories.get(origin);
		if (repository == null)
			throw new IllegalArgumentException(
					"Callimachus not serving: " + origin);
		String pipe = repository.getCallimachusUrl(origin, path);
		if (pipe == null)
			throw new IllegalArgumentException(
					"Callimachus webapp not setup on: " + origin);
		server.setErrorPipe(pipe);
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

	public void listen(int[] ports, int[] sslports) throws OpenRDFException, IOException {
		assert ports != null && ports.length > 0 || sslports != null
				&& sslports.length > 0;
		if (ports == null) {
			ports = new int[0];
		} else if (sslports == null) {
			sslports = new int[0];
		}
		server.listen(ports, sslports);
	}

	public void start() throws IOException, OpenRDFException {
		logger.info("Callimachus is binding to {}", toString());
		for (String origin : repositories.keySet()) {
			HttpHost host = getAuthorityAddress(origin);
			HTTPObjectClient.getInstance().setProxy(host, server);
		}
		for (CalliRepository repository : repositories.values()) {
			repository.setCompileRepository(true);
		}
		server.start();
		System.gc();
		Thread.yield();
		long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
		logger.info("Callimachus started after {} seconds", uptime / 1000.0);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String origin : repositories.keySet()) {
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

	public void stop() throws IOException {
		server.stop();
	}

	public void destroy() throws IOException {
		server.destroy();
		for (String origin : repositories.keySet()) {
			HttpHost host = getAuthorityAddress(origin);
			HTTPObjectClient.getInstance().removeProxy(host, server);
		}
	}

	private HttpHost getAuthorityAddress(String origin) {
		return URIUtils.extractHost(java.net.URI.create(origin + "/"));
	}

	private HTTPObjectServer createServer(File dir)
			throws IOException, NoSuchAlgorithmException {
		HTTPObjectServer server = new HTTPObjectServer(dir);
		server.setEnvelopeType(ENVELOPE_TYPE);
		return server;
	}

}
