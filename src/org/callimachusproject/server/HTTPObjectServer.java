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
import static org.apache.http.params.CoreConnectionPNames.SO_KEEPALIVE;
import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.STALE_CONNECTION_CHECK;
import static org.apache.http.params.CoreConnectionPNames.TCP_NODELAY;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.impl.nio.SSLNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerRegistry;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.callimachusproject.Version;
import org.callimachusproject.client.AbstractHttpClient;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.concurrent.ManagedExecutors;
import org.callimachusproject.concurrent.NamedThreadFactory;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.cache.CachingFilter;
import org.callimachusproject.server.filters.GUnzipFilter;
import org.callimachusproject.server.filters.GZipFilter;
import org.callimachusproject.server.filters.HeadRequestFilter;
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
import org.callimachusproject.server.process.Exchange;
import org.callimachusproject.server.process.HTTPObjectRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the start and stop stages of the server.
 * 
 * @author James Leigh
 * @param <a>
 * 
 */
public class HTTPObjectServer extends AbstractHttpClient implements HTTPObjectAgentMXBean, IOReactorExceptionHandler {
	protected static final String DEFAULT_NAME = Version.getInstance().getVersion();
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

	private final Logger logger = LoggerFactory.getLogger(HTTPObjectServer.class);
	private final Set<NHttpConnection> connections = new HashSet<NHttpConnection>();
	private final DefaultListeningIOReactor server;
	private final IOEventDispatch dispatch;
	private DefaultListeningIOReactor sslserver;
	private IOEventDispatch ssldispatch;
	private int[] ports;
	private int[] sslports;
	private final ServerNameFilter name;
	private final IdentityPrefix abs;
	private final HttpResponseFilter env;
	private CountDownLatch started;
	private CountDownLatch stopped;
	private final HTTPObjectRequestHandler service;
	private final LinksHandler links;
	private final AuthenticationHandler authCache;
	private final ModifiedSinceHandler remoteCache;
	private final CachingFilter cache;
	private int timeout = 0;
	private final HttpProcessor httpproc;

	public HTTPObjectServer(File cacheDir)
			throws IOException, NoSuchAlgorithmException {
		Handler handler = new InvokeHandler();
		handler = new NotFoundHandler(handler);
		handler = new AlternativeHandler(handler);
		handler = new ResponseExceptionHandler(handler);
		handler = new OptionsHandler(handler);
		handler = links = new LinksHandler(handler);
		handler = remoteCache = new ModifiedSinceHandler(handler);
		handler = new UnmodifiedSinceHandler(handler);
		handler = new ContentHeadersHandler(handler);
		handler = authCache = new AuthenticationHandler(handler);
		Filter filter = env = new HttpResponseFilter(null);
		filter = new GZipFilter(filter);
		filter = cache = new CachingFilter(filter, cacheDir, 1024);
		filter = new GUnzipFilter(filter);
		filter = new MD5ValidationFilter(filter);
		filter = abs = new IdentityPrefix(filter);
		filter = new TraceFilter(filter);
		filter = name = new ServerNameFilter(DEFAULT_NAME, filter);
		filter = new HeadRequestFilter(filter);
		filter = new AccessLog(filter);
		service = new HTTPObjectRequestHandler(filter, handler);
		httpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] {
				new ResponseDate(), new ResponseContent(true),
				new ResponseConnControl() });
		// Create server-side I/O event dispatch
		dispatch = createIODispatch();
		// Create server-side I/O reactor
		server = new DefaultListeningIOReactor();
		server.setExceptionHandler(this);
		if (System.getProperty("javax.net.ssl.keyStore") != null) {
			try {
				SSLContext sslcontext = SSLContext.getDefault();
				// Create server-side I/O event dispatch
				ssldispatch = createSSLDispatch(sslcontext);
				// Create server-side I/O reactor
				sslserver = new DefaultListeningIOReactor();
				sslserver.setExceptionHandler(this);
			} catch (NoSuchAlgorithmException e) {
				logger.warn(e.toString(), e);
			}
		}
	}

	public synchronized void addOrigin(String origin, CalliRepository repository) {
		service.addOrigin(origin, repository);
		authCache.addOrigin(origin, repository);
		repository.addSchemaListener(new Runnable() {
			public String toString() {
				return "reset cache";
			}

			public void run() {
				resetCache();
			}
		});
	}

	private DefaultHttpServerIODispatch createIODispatch() {
		HttpAsyncService handler;
		DefaultNHttpServerConnectionFactory factory;
		HttpParams params = getDefaultHttpParams();
		params.setParameter("http.protocol.scheme", "http");
		handler = createProtocolHandler(httpproc, service, params);
		factory = new DefaultNHttpServerConnectionFactory(params);
		return new DefaultHttpServerIODispatch(handler, factory);
	}

	private DefaultHttpServerIODispatch createSSLDispatch(SSLContext sslcontext) {
		HttpAsyncService handler;
		SSLNHttpServerConnectionFactory factory;
		HttpParams params = getDefaultHttpParams();
		params.setParameter("http.protocol.scheme", "https");
		handler = createProtocolHandler(httpproc, service, params);
		factory = new SSLNHttpServerConnectionFactory(sslcontext, null, params);
		return new DefaultHttpServerIODispatch(handler, factory);
	}

	private HttpParams getDefaultHttpParams() {
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(SO_TIMEOUT, timeout);
		params.setBooleanParameter(SO_KEEPALIVE, true);
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024);
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false);
		params.setBooleanParameter(TCP_NODELAY, false);
		return params;
	}

	private HttpAsyncService createProtocolHandler(HttpProcessor httpproc,
			HTTPObjectRequestHandler service, HttpParams params) {
		// Create request handler registry
        HttpAsyncRequestHandlerRegistry reqistry = new HttpAsyncRequestHandlerRegistry();
        // Register the default handler for all URIs
        reqistry.register("*", service);
        // Create server-side HTTP protocol handler
        HttpAsyncService protocolHandler = new HttpAsyncService(
                httpproc, new DefaultConnectionReuseStrategy(), reqistry, params) {

            @Override
            public void connected(final NHttpServerConnection conn) {
                super.connected(conn);
                synchronized (connections) {
                	connections.add(conn);
                }
            }

            @Override
            public void closed(final NHttpServerConnection conn) {
            	synchronized (connections) {
            		connections.remove(conn);
            	}
                super.closed(conn);
            }

        };
		return protocolHandler;
	}

	public String getErrorPipe() {
		return service.getErrorPipe();
	}

	public void setErrorPipe(String url) throws IOException {
		service.setErrorPipe(url);
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

	public void setEnvelopeType(String type) {
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
		HTTPObjectClient.invalidateCache();
		remoteCache.invalidate();
	}

	public void resetCache() {
		ManagedExecutors.getInstance().getTimeoutThreadPool().execute(new Runnable() {
			public String toString() {
				return "reset cache";
			}

			public void run() {
				try {
					cache.reset();
					HTTPObjectClient.resetCache();
					remoteCache.invalidate();
					authCache.resetCache();
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
		NHttpConnection[] connections = getOpenConnections();
		for (int i = 0; i < connections.length; i++) {
			connections[i].shutdown();
		}
	}

	public synchronized void listen(int[] ports, int[] sslports) throws IOException {
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
		int count = (ports.length > 0 ? 1 : 0) + (sslserver != null && sslports.length > 0 ? 1 : 0);
		this.started = new CountDownLatch(count);
		this.stopped = new CountDownLatch(count);
		if (ports.length > 0) {
			for (int port : ports) {
				server.listen(new InetSocketAddress(port));
			}
			server.pause();
			executor.newThread(new Runnable() {
				public void run() {
					try {
						started.countDown();
						server.execute(dispatch);
					} catch (IOException e) {
						logger.error(e.toString(), e);
					} finally {
						stopped.countDown();
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
						started.countDown();
						sslserver.execute(ssldispatch);
					} catch (IOException e) {
						logger.error(e.toString(), e);
					} finally {
						stopped.countDown();
					}
				}
			}).start();
		}
		try {
			started.await();
			Thread.sleep(100);
			if (ports.length > 0 && server != null
					&& server.getStatus() != IOReactorStatus.ACTIVE
					|| sslports.length > 0 && sslserver != null
					&& sslserver.getStatus() != IOReactorStatus.ACTIVE) {
				String str = Arrays.toString(ports) + Arrays.toString(sslports);
				str = str.replace('[', ' ').replace(']', ' ');
				throw new BindException("Could not bind to port" + str
						+ "server is " + getStatus());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		synchronized (instances) {
			instances.add(this);
		}
	}

	public synchronized void start() throws IOException {
		service.start();
		if (ports.length > 0) {
			server.resume();
		}
		if (sslserver != null && sslports.length > 0) {
			sslserver.resume();
		}
	}

	public boolean isRunning() {
		if (ports == null || sslports == null)
			return false;
		if (ports.length > 0 && server.getStatus() == IOReactorStatus.ACTIVE)
			return !service.isShutdown();
		if (sslports.length > 0 && sslserver != null
				&& sslserver.getStatus() == IOReactorStatus.ACTIVE)
			return !service.isShutdown();
		return false;
	}

	public synchronized void stop() throws IOException {
		service.stop();
		if (ports.length > 0) {
			server.pause();
		}
		if (sslserver != null && sslports.length > 0) {
			sslserver.pause();
		}
	}

	public synchronized void destroy() throws IOException {
		stop();
		server.shutdown();
		if (sslserver != null) {
			sslserver.shutdown();
		}
		resetConnections();
		try {
			stopped.await();
			Thread.sleep(100);
			while (server.getStatus() != IOReactorStatus.SHUT_DOWN
					&& server.getStatus() != IOReactorStatus.INACTIVE) {
				Thread.sleep(1000);
				if (isRunning())
					throw new IOException("Could not shutdown server");
			}
			if (sslserver != null) {
				while (sslserver.getStatus() != IOReactorStatus.SHUT_DOWN
						&& sslserver.getStatus() != IOReactorStatus.INACTIVE) {
					Thread.sleep(1000);
					if (isRunning())
						throw new IOException("Could not shutdown server");
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		synchronized (instances) {
			instances.remove(this);
		}
	}

	@Override
	public boolean handle(IOException ex) {
		logger.warn(ex.toString());
		return true;
	}

	@Override
	public boolean handle(RuntimeException ex) {
		logger.error(ex.toString(), ex);
		return true;
	}

	public void poke() {
		System.gc();
		for (NHttpConnection conn : getOpenConnections()) {
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

	@Override
	public HttpResponse execute(HttpHost host, HttpRequest request,
			HttpContext context) throws IOException, ClientProtocolException {
		try {
			if (context == null)
				context = new BasicHttpContext();
			httpproc.process(request, context);
			HttpResponse response = service.execute(host, request, context);
			httpproc.process(response, context);
			return response;
		} catch (HttpException e) {
			throw new ClientProtocolException(e);
		}
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return service.getConnectionManager();
	}

	@Override
	public HttpParams getParams() {
		return service.getParams();
	}

	public ConnectionBean[] getConnections() {
		NHttpConnection[] connections = getOpenConnections();
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
			Exchange[] array = service.getPendingExchange(ctx);
			if (array != null) {
				String[] pending = new String[array.length];
				for (int j=0;j<pending.length;j++) {
					pending[j] = array[j].toString();
					if (array[j].isReadingRequest()) {
						bean.setConsuming(array[j].toString());
					}
				}
				bean.setPending(pending);
			}
		}
		return beans;
	}

	public void connectionDumpToFile(String outputFile) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true));
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
				writer.println();
			}
			writer.println();
			writer.println();
		} finally {
			writer.close();
		}
		logger.info("Connection dump: {}", outputFile);
	}

	private NHttpConnection[] getOpenConnections() {
		synchronized (connections) {
			return connections.toArray(new NHttpConnection[connections.size()]);
		}
	}

	private String toString(String string) {
		if (string == null)
			return "";
		return string;
	}

}
