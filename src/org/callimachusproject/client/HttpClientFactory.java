package org.callimachusproject.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.client.cache.FileResourceFactory;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.Version;
import org.callimachusproject.io.FileUtil;
import org.callimachusproject.util.MailProperties;

/**
 * Manages the connections and cache entries for outgoing requests.
 * 
 * @author James Leigh
 * 
 */
public class HttpClientFactory implements Closeable {
	private static final int KEEPALIVE = 4000;
	private static final String DEFAULT_NAME = Version.getInstance()
			.getVersion();
	private static HttpClientFactory instance;
	static {
		try {
			String tmpDirStr = System.getProperty("java.io.tmpdir");
			if (tmpDirStr != null) {
				File tmpDir = new File(tmpDirStr);
				if (!tmpDir.exists()) {
					tmpDir.mkdirs();
				}
			}
			File dir = File.createTempFile("http-client-cache", "");
			dir.delete();
			FileUtil.deleteOnExit(dir);
			setCacheDirectory(dir);
			instance = new HttpClientFactory(dir);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				synchronized (HttpClientFactory.class) {
					if (instance != null) {
						instance.close();
						instance = null;
					}
				}
			}
		}));
	}

	public static synchronized HttpClientFactory getInstance() {
		return instance;
	}

	public static synchronized void setCacheDirectory(File dir)
			throws IOException {
		if (instance != null) {
			instance.close();
		}
		instance = new HttpClientFactory(dir);
	}

	private final ProxyClientExecDecorator decorator;
	private final ResourceFactory entryFactory;
	final PoolingHttpClientConnectionManager connManager;
	private final ConnectionReuseStrategy reuseStrategy;
	private final ConnectionKeepAliveStrategy keepAliveStrategy;

	private HttpClientFactory(File cacheDir) throws IOException {
		cacheDir.mkdirs();
		entryFactory = new FileResourceFactory(cacheDir);
		decorator = new ProxyClientExecDecorator();
		LayeredConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactory
				.getSystemSocketFactory();
		connManager = new PoolingHttpClientConnectionManager(RegistryBuilder
				.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslSocketFactory).build());
		connManager.setDefaultSocketConfig(getDefaultSocketConfig());
		connManager.setDefaultConnectionConfig(getDefaultConnectionConfig());
		int max = Integer.parseInt(System.getProperty("http.maxConnections", "20"));
		connManager.setDefaultMaxPerRoute(max);
		connManager.setMaxTotal(2 * max);
		reuseStrategy = DefaultConnectionReuseStrategy.INSTANCE;
		keepAliveStrategy = new ConnectionKeepAliveStrategy() {
			private ConnectionKeepAliveStrategy delegate = DefaultConnectionKeepAliveStrategy.INSTANCE;

			public long getKeepAliveDuration(HttpResponse response,
					HttpContext context) {
				long ret = delegate.getKeepAliveDuration(response, context);
				if (ret > 0)
					return ret;
				return KEEPALIVE;
			}
		};
	}

	public synchronized void close() {
		connManager.shutdown();
	}

	public synchronized ClientExecChain getProxy(HttpHost destination) {
		return decorator.getProxy(destination);
	}

	public synchronized ClientExecChain setProxy(HttpHost destination,
			ClientExecChain proxy) {
		return decorator.setProxy(destination, proxy);
	}

	public synchronized ClientExecChain setProxyIfAbsent(HttpHost destination,
			ClientExecChain proxy) {
		return decorator.setProxyIfAbsent(destination, proxy);
	}

	public synchronized boolean removeProxy(HttpHost destination,
			ClientExecChain proxy) {
		return decorator.removeProxy(destination, proxy);
	}

	public synchronized boolean removeProxy(ClientExecChain proxy) {
		return decorator.removeProxy(proxy);
	}

	public CloseableHttpClient createHttpClient(String source) {
		return createHttpClient(source, new SystemDefaultCredentialsProvider());
	}

	public synchronized CloseableHttpClient createHttpClient(String source, CredentialsProvider credentials) {
		CacheConfig cache = getDefaultCacheConfig();
		ManagedHttpCacheStorage storage = new ManagedHttpCacheStorage(cache);
		List<BasicHeader> headers = new ArrayList<BasicHeader>();
		headers.add(new BasicHeader("Origin", getOrigin(source)));
		headers.addAll(getAdditionalRequestHeaders());
		return new AutoClosingHttpClient(new CachingHttpClientBuilder() {
			protected ClientExecChain decorateMainExec(ClientExecChain mainExec) {
				return super.decorateMainExec(decorator
						.decorateMainExec(mainExec));
			}
		}.setResourceFactory(entryFactory).setHttpCacheStorage(storage)
				.setCacheConfig(cache)
				.setConnectionManager(getConnectionManager())
				.setConnectionReuseStrategy(reuseStrategy)
				.setKeepAliveStrategy(keepAliveStrategy).useSystemProperties()
				.disableContentCompression()
				.setDefaultRequestConfig(getDefaultRequestConfig())
				.addInterceptorFirst(new GZipInterceptor())
				.addInterceptorFirst(new GUnzipInterceptor())
				.setDefaultCredentialsProvider(credentials)
				.setDefaultHeaders(headers).setUserAgent(DEFAULT_NAME).build(),
				storage);
	}

	private HttpClientConnectionManager getConnectionManager() {
		return new HttpClientConnectionManager() {
			public ConnectionRequest requestConnection(HttpRoute route,
					Object state) {
				return connManager.requestConnection(route, state);
			}

			public void releaseConnection(HttpClientConnection conn,
					Object newState, long validDuration, TimeUnit timeUnit) {
				connManager.releaseConnection(conn, newState, validDuration,
						timeUnit);
			}

			public void connect(HttpClientConnection conn, HttpRoute route,
					int connectTimeout, HttpContext context) throws IOException {
				connManager.connect(conn, route, connectTimeout, context);
			}

			public void upgrade(HttpClientConnection conn, HttpRoute route,
					HttpContext context) throws IOException {
				connManager.upgrade(conn, route, context);
			}

			public void routeComplete(HttpClientConnection conn,
					HttpRoute route, HttpContext context) throws IOException {
				connManager.routeComplete(conn, route, context);
			}

			public void closeIdleConnections(long idletime, TimeUnit tunit) {
				connManager.closeIdleConnections(idletime, tunit);
			}

			public void closeExpiredConnections() {
				connManager.closeExpiredConnections();
			}

			public void shutdown() {
				// connection manager is closed elsewhere
			}
		};
	}

	private String getOrigin(String source) {
		assert source != null;
		int scheme = source.indexOf("://");
		if (scheme < 0 && (source.startsWith("file:") || source.startsWith("jar:file:"))) {
			return "file://";
		} else {
			if (scheme < 0)
				throw new IllegalArgumentException("Not an absolute hierarchical URI: " + source);
			int path = source.indexOf('/', scheme + 3);
			if (path >= 0) {
				return source.substring(0, path);
			} else {
				return source;
			}
		}
	}

	private RequestConfig getDefaultRequestConfig() {
		return RequestConfig.custom().setSocketTimeout(0)
				.setConnectTimeout(10000).setStaleConnectionCheckEnabled(false)
				.setExpectContinueEnabled(true).setMaxRedirects(20)
				.setRedirectsEnabled(true).setCircularRedirectsAllowed(false)
				.build();
	}

	private ConnectionConfig getDefaultConnectionConfig() {
		return ConnectionConfig.custom().setBufferSize(8 * 1024).build();
	}

	private SocketConfig getDefaultSocketConfig() {
		return SocketConfig.custom().setTcpNoDelay(false)
				.setSoTimeout(60 * 1000).build();
	}

	private Collection<BasicHeader> getAdditionalRequestHeaders() {
		try {
			MailProperties mail = MailProperties.getInstance();
			String from = mail.getMailProperties().get("mail.from");
			if (from != null) {
				BasicHeader hd = new BasicHeader("From", from);
				return Collections.singleton(hd);
			}
		} catch (IOException e) {
			// ignore
		} catch (SecurityException e) {
			// ignore
		}
		return Collections.emptySet();
	}

	private CacheConfig getDefaultCacheConfig() {
		return CacheConfig.custom().setSharedCache(false)
				.setAllow303Caching(true)
				.setWeakETagOnPutDeleteAllowed(true)
				.setHeuristicCachingEnabled(true)
				.setHeuristicDefaultLifetime(60 * 60 * 24)
				.setMaxObjectSize(1024 * 1024).build();
	}
}
