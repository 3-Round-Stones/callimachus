/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.callimachusproject.server;

import static org.apache.http.params.CoreConnectionPNames.SOCKET_BUFFER_SIZE;
import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.STALE_CONNECTION_CHECK;
import static org.apache.http.params.CoreConnectionPNames.TCP_NODELAY;
import static org.callimachusproject.server.HTTPObjectRequestHandler.HANDLER_ATTR;
import static org.callimachusproject.server.HTTPObjectRequestHandler.PENDING_ATTR;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.activation.MimeTypeParseException;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.AsyncNHttpServiceHandler;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpRequestHandlerResolver;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.Version;
import org.callimachusproject.server.cache.CachingFilter;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.client.HTTPService;
import org.callimachusproject.server.filters.DateHeaderFilter;
import org.callimachusproject.server.filters.GUnzipFilter;
import org.callimachusproject.server.filters.GZipFilter;
import org.callimachusproject.server.filters.HttpResponseFilter;
import org.callimachusproject.server.filters.IdentityPrefix;
import org.callimachusproject.server.filters.MD5ValidationFilter;
import org.callimachusproject.server.filters.ServerNameFilter;
import org.callimachusproject.server.filters.TraceFilter;
import org.callimachusproject.server.handlers.AlternativeHandler;
import org.callimachusproject.server.handlers.AuthenticationHandler;
import org.callimachusproject.server.handlers.ContentHeadersHandler;
import org.callimachusproject.server.handlers.InvokeHandler;
import org.callimachusproject.server.handlers.LinksHandler;
import org.callimachusproject.server.handlers.ModifiedSinceHandler;
import org.callimachusproject.server.handlers.NotFoundHandler;
import org.callimachusproject.server.handlers.OptionsHandler;
import org.callimachusproject.server.handlers.ResponseExceptionHandler;
import org.callimachusproject.server.handlers.UnmodifiedSinceHandler;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.tasks.Task;
import org.callimachusproject.server.util.ManagedExecutors;
import org.callimachusproject.server.util.NamedThreadFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the start and stop stages of the server.
 * 
 * @author James Leigh
 * @param <a>
 * 
 */
public class HTTPObjectServer implements HTTPService, HTTPObjectAgentMXBean {
	private static final String APP_NAME = "Callimachus Server";
	protected static final String DEFAULT_NAME = APP_NAME + "/" + Version.getVersion();
	private static NamedThreadFactory executor = new NamedThreadFactory("HttpObjectServer", false);
	private static final List<HTTPObjectServer> instances = new ArrayList<HTTPObjectServer>();

	public static HTTPObjectServer[] getInstances() {
		synchronized (instances) {
			return instances.toArray(new HTTPObjectServer[instances.size()]);
		}
	}

	public static void resetAllCache() {
		for (HTTPObjectServer server : getInstances()) {
			server.resetCache();
		}
	}

	private Logger logger = LoggerFactory.getLogger(HTTPObjectServer.class);
	private ListeningIOReactor server;
	private IOEventDispatch dispatch;
	private ListeningIOReactor sslserver;
	private IOEventDispatch ssldispatch;
	private ObjectRepository repository;
	private int[] ports;
	private int[] sslports;
	private ServerNameFilter name;
	private IdentityPrefix abs;
	private HttpResponseFilter env;
	private boolean started = false;
	private boolean stopped = true;
	private HTTPObjectRequestHandler service;
	private LinksHandler links;
	private ModifiedSinceHandler remoteCache;
	private CachingFilter cache;
	private int timeout = 0;

	/**
	 * @param basic
	 *            username:password
	 */
	public HTTPObjectServer(ObjectRepository repository, File cacheDir,
			String basic) throws IOException, NoSuchAlgorithmException {
		this.repository = repository;
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(SO_TIMEOUT, timeout);
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024);
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false);
		params.setBooleanParameter(TCP_NODELAY, false);
		int n = Runtime.getRuntime().availableProcessors();
		Handler handler = new InvokeHandler();
		handler = new NotFoundHandler(handler);
		handler = new AlternativeHandler(handler);
		handler = new ResponseExceptionHandler(handler);
		handler = new OptionsHandler(handler);
		handler = links = new LinksHandler(handler);
		handler = remoteCache = new ModifiedSinceHandler(handler);
		handler = new UnmodifiedSinceHandler(handler);
		handler = new ContentHeadersHandler(handler);
		handler = new AuthenticationHandler(handler, basic);
		Filter filter = env = new HttpResponseFilter(null);
		filter = new DateHeaderFilter(filter);
		filter = new GZipFilter(filter);
		filter = cache = new CachingFilter(filter, cacheDir, 1024);
		filter = new GUnzipFilter(filter);
		filter = new MD5ValidationFilter(filter);
		filter = abs = new IdentityPrefix(filter);
		filter = new TraceFilter(filter);
		filter = name = new ServerNameFilter(DEFAULT_NAME, filter);
		service = new HTTPObjectRequestHandler(filter, handler, repository);
		AsyncNHttpServiceHandler async = new AsyncNHttpServiceHandler(
				new BasicHttpProcessor(), new DefaultHttpResponseFactory(),
				new DefaultConnectionReuseStrategy(), params);
		async.setExpectationVerifier(service);
		async.setEventListener(service);
		async.setHandlerResolver(new NHttpRequestHandlerResolver() {
			public NHttpRequestHandler lookup(String requestURI) {
				return service;
			}
		});
		dispatch = new HTTPServerIOEventDispatch(async, params);
		server = new DefaultListeningIOReactor(n, params);
		if (System.getProperty("javax.net.ssl.keyStore") != null) {
			SSLContext ssl = SSLContext.getDefault();
			ssldispatch = new HTTPSServerIOEventDispatch(async, ssl, params);
			sslserver = new DefaultListeningIOReactor(n, params);
		}
		repository.addSchemaListener(new Runnable() {
			public String toString() {
				return "reset cache";
			}
			public void run() {
				resetCache();
			}
		});
	}

	public String getErrorXSLT() {
		return service.getErrorXSLT();
	}

	public void setErrorXSLT(String url) {
		service.setErrorXSLT(url);
	}

	public Repository getRepository() {
		return repository;
	}

	public String getName() {
		return name.getServerName();
	}

	public void setName(String serverName) {
		this.name.setServerName(serverName);
	}

	public String[] getIdentityPrefix() {
		return abs.getIdentityPrefix();
	}

	/**
	 * Defines the keep alive timeout in milliseconds, which is the timeout for
	 * waiting for data. A timeout value of zero is interpreted as an infinite
	 * timeout.
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Defines the keep alive timeout in milliseconds, which is the timeout for
	 * waiting for data. A timeout value of zero is interpreted as an infinite
	 * timeout.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setIdentityPrefix(String[] prefix) {
		abs.setIdentityPrefix(prefix);
	}

	public String getEnvelopeType() {
		return env.getEnvelopeType();
	}

	public void setEnvelopeType(String type) throws MimeTypeParseException {
		env.setEnvelopeType(type);
		links.setEnvelopeType(type);
	}

	public boolean isCacheAggressive() {
		return cache.isAggressive();
	}

	public boolean isCacheDisconnected() {
		return cache.isDisconnected();
	}

	public boolean isCacheEnabled() {
		return cache.isEnabled();
	}

	public void setCacheAggressive(boolean cacheAggressive) {
		cache.setAggressive(cacheAggressive);
	}

	public void setCacheDisconnected(boolean cacheDisconnected) {
		cache.setDisconnected(cacheDisconnected);
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		cache.setEnabled(cacheEnabled);
	}

	public int getCacheCapacity() {
		return cache.getMaxCapacity();
	}

	public void setCacheCapacity(int capacity) {
		cache.setMaxCapacity(capacity);
	}

	public int getCacheSize() {
		return cache.getSize();
	}

	public String getFrom() {
		return null;
	}

	public void setFrom(String from) {
		throw new UnsupportedOperationException();
	}

	public void invalidateCache() throws IOException, InterruptedException {
		cache.invalidate();
		HTTPObjectClient.getInstance().invalidateCache();
		remoteCache.invalidate();
	}

	public void resetCache() {
		ManagedExecutors.getTimeoutThreadPool().execute(new Runnable() {
			public String toString() {
				return "reset cache";
			}

			public void run() {
				try {
					cache.reset();
					HTTPObjectClient.getInstance().resetCache();
					remoteCache.invalidate();
				} catch (Error e) {
					logger.error(e.toString(), e);
				} catch (RuntimeException e) {
					logger.error(e.toString(), e);
				} catch (IOException e) {
					logger.error(e.toString(), e);
				} catch (InterruptedException e) {
					logger.info(e.toString(), e);
				}
			}
		});
	}

	public void resetConnections() throws IOException {
		NHttpConnection[] connections = service.getConnections();
		for (int i = 0; i < connections.length; i++) {
			connections[i].shutdown();
		}
	}

	public synchronized void listen(int[] ports, int[] sslports) throws Exception {
		if (ports == null) {
			ports = new int[0];
		}
		if (sslports == null) {
			sslports = new int[0];
		}
		if (ports.length <= 0 && sslports.length <= 0)
			throw new IllegalStateException("No ports to listen on");
		if (ports.length <= 0 && sslserver == null)
			throw new IllegalStateException("No configured keystore for SSL ports");
		if (isRunning())
			throw new IllegalStateException("Server is already running");
		name.setPort(ports.length > 0 ? ports[0] : sslports[0]);
		this.ports = ports;
		this.sslports = sslports;
		started = false;
		stopped = false;
		if (ports.length > 0) {
			for (int port : ports) {
				server.listen(new InetSocketAddress(port));
			}
			server.pause();
			executor.newThread(new Runnable() {
				public void run() {
					try {
						synchronized (HTTPObjectServer.this) {
							started = true;
							HTTPObjectServer.this.notifyAll();
						}
						server.execute(dispatch);
					} catch (IOException e) {
						logger.error(e.toString(), e);
					} finally {
						synchronized (HTTPObjectServer.this) {
							stopped = true;
							HTTPObjectServer.this.notifyAll();
						}
					}
				}
			}).start();
		}
		if (sslserver != null && sslports.length > 0) {
			for (int port : sslports) {
				sslserver.listen(new InetSocketAddress(port));
			}
			sslserver.pause();
			executor.newThread(new Runnable() {
				public void run() {
					try {
						synchronized (HTTPObjectServer.this) {
							started = true;
							HTTPObjectServer.this.notifyAll();
						}
						sslserver.execute(ssldispatch);
					} catch (IOException e) {
						logger.error(e.toString(), e);
					} finally {
						synchronized (HTTPObjectServer.this) {
							stopped = true;
							HTTPObjectServer.this.notifyAll();
						}
					}
				}
			}).start();
		}
		while (!started) {
			wait();
		}
		Thread.sleep(100);
		if (!isRunning()) {
			String str = Arrays.toString(ports) + Arrays.toString(sslports);
			str = str.replace('[', ' ').replace(']', ' ');
			throw new BindException("Could not bind to port" + str
					+ "server is " + getStatus());
		}
		synchronized (instances) {
			instances.add(this);
		}
	}

	public synchronized void start() throws Exception {
		if (ports.length > 0) {
			for (int port : ports) {
				registerService(HTTPObjectClient.getInstance(), port);
			}
			server.resume();
		}
		if (sslserver != null && sslports.length > 0) {
			for (int port : sslports) {
				registerService(HTTPObjectClient.getInstance(), port);
			}
			sslserver.resume();
		}
	}

	public boolean isRunning() {
		if (stopped)
			return false;
		if (ports.length > 0 && server.getStatus() == IOReactorStatus.ACTIVE)
			return true;
		if (sslports.length > 0 && sslserver != null
				&& sslserver.getStatus() == IOReactorStatus.ACTIVE)
			return true;
		return false;
	}

	public synchronized void stop() throws Exception {
		if (ports.length > 0) {
			for (int port : ports) {
				deregisterService(HTTPObjectClient.getInstance(), port);
			}
			server.pause();
		}
		if (sslserver != null && sslports.length > 0) {
			for (int port : sslports) {
				deregisterService(HTTPObjectClient.getInstance(), port);
			}
			sslserver.pause();
		}
	}

	public synchronized void destroy() throws Exception {
		stop();
		server.shutdown();
		if (sslserver != null) {
			sslserver.shutdown();
		}
		resetConnections();
		while (!stopped) {
			wait();
		}
		Thread.sleep(100);
		while (server.getStatus() != IOReactorStatus.SHUT_DOWN
				&& server.getStatus() != IOReactorStatus.INACTIVE) {
			Thread.sleep(1000);
			if (isRunning())
				throw new HttpException("Could not shutdown server");
		}
		if (sslserver != null) {
			while (sslserver.getStatus() != IOReactorStatus.SHUT_DOWN
					&& sslserver.getStatus() != IOReactorStatus.INACTIVE) {
				Thread.sleep(1000);
				if (isRunning())
					throw new HttpException("Could not shutdown server");
			}
		}
		synchronized (instances) {
			instances.remove(this);
		}
	}

	public void poke() {
		System.gc();
		for (NHttpConnection conn : service.getConnections()) {
			conn.requestInput();
			conn.requestOutput();
		}
	}

	public String getStatus() {
		StringBuilder sb = new StringBuilder();
		if (ports.length > 0) {
			sb.append(server.getStatus().toString());
		}
		if (ports.length > 0 && sslports.length > 0) {
			sb.append(", ssl: ");
		}
		if (sslserver != null && sslports.length > 0) {
			sb.append(sslserver.getStatus().toString());
		}
		return sb.toString();
	}

	public HttpResponse service(HttpRequest request) throws IOException {
		return service.service(request);
	}

	public ConnectionBean[] getConnections() {
		NHttpConnection[] connections = service.getConnections();
		ConnectionBean[] beans = new ConnectionBean[connections.length];
		for (int i = 0; i < beans.length; i++) {
			ConnectionBean bean = new ConnectionBean();
			NHttpConnection conn = connections[i];
			beans[i] = bean;
			switch (conn.getStatus()) {
			case NHttpConnection.ACTIVE:
				if (conn.isOpen()) {
					bean.setStatus("OPEN");
				} else if (conn.isStale()) {
					bean.setStatus("STALE");
				} else {
					bean.setStatus("ACTIVE");
				}
				break;
			case NHttpConnection.CLOSING:
				bean.setStatus("CLOSING");
				break;
			case NHttpConnection.CLOSED:
				bean.setStatus("CLOSED");
				break;
			}
			if (conn instanceof HttpInetConnection) {
				HttpInetConnection inet = (HttpInetConnection) conn;
				InetAddress ra = inet.getRemoteAddress();
				int rp = inet.getRemotePort();
				InetAddress la = inet.getLocalAddress();
				int lp = inet.getLocalPort();
				InetSocketAddress remote = new InetSocketAddress(ra, rp);
				InetSocketAddress local = new InetSocketAddress(la, lp);
				bean.setStatus(bean.getStatus() + " " + remote + "->" + local);
			}
			HttpRequest req = conn.getHttpRequest();
			if (req != null) {
				bean.setRequest(req.getRequestLine().toString());
			}
			HttpResponse resp = conn.getHttpResponse();
			if (resp != null) {
				bean.setResponse(resp.getStatusLine().toString() + " "
						+ resp.getEntity());
			}
			HttpContext ctx = conn.getContext();
			Object handler = ctx.getAttribute(HANDLER_ATTR);
			if (handler != null) {
				bean.setConsuming(handler.toString());
			}
			Queue queue = (Queue) ctx.getAttribute(PENDING_ATTR);
			if (queue != null) {
				Object[] array = null;
				synchronized (queue) {
					if (!queue.isEmpty()) {
						array = queue.toArray(new Task[queue.size()]);
					}
				}
				if (array != null) {
					String[] pending = new String[queue.size()];
					for (int j=0;j<pending.length;j++) {
						pending[j] = array[j].toString();
					}
					bean.setPending(pending);
				}
			}
		}
		return beans;
	}

	public void connectionDumpToFile(String outputFile) throws IOException {
		PrintWriter writer = new PrintWriter(outputFile);
		try {
			writer.println("status,request,consuming,response,pending");
			for (ConnectionBean connection : getConnections()) {
				writer.print(toString(connection.getStatus()));
				writer.print(",");
				writer.print(toString(connection.getRequest()));
				writer.print(",");
				writer.print(toString(connection.getConsuming()));
				writer.print(",");
				writer.print(toString(connection.getResponse()));
				writer.print(",");
				String[] pending = connection.getPending();
				if (pending != null) {
					for(String p : pending) {
						writer.print(toString(p));
						writer.print(",");
					}
				}
			}
		} finally {
			writer.close();
		}
		logger.info("Connection dump: {}", outputFile);
	}

	private String toString(String string) {
		if (string == null)
			return "";
		return string;
	}

	private void registerService(HTTPObjectClient client, int port) {
		for (InetAddress addr : getAllLocalAddresses()) {
			client.setProxy(new InetSocketAddress(addr, port), service);
		}
	}

	private void deregisterService(HTTPObjectClient client, int port) {
		for (InetAddress addr : getAllLocalAddresses()) {
			client.removeProxy(new InetSocketAddress(addr, port), service);
		}
	}

	private Set<InetAddress> getAllLocalAddresses() {
		Set<InetAddress> result = new HashSet<InetAddress>();
		try {
			result.addAll(Arrays.asList(InetAddress.getAllByName(null)));
		} catch (UnknownHostException e) {
			// no loop back device
		}
		try {
			InetAddress local = InetAddress.getLocalHost();
			result.add(local);
			try {
				result.addAll(Arrays.asList(InetAddress.getAllByName(local
						.getCanonicalHostName())));
			} catch (UnknownHostException e) {
				// no canonical name
			}
		} catch (UnknownHostException e) {
			// no network
		}
		try {
			Enumeration<NetworkInterface> interfaces;
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces != null && interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				Enumeration<InetAddress> addrs = iface.getInetAddresses();
				while (addrs != null && addrs.hasMoreElements()) {
					result.add(addrs.nextElement());
				}
			}
		} catch (SocketException e) {
			// broken network configuration
		}
		return result;
	}

}
