package org.callimachusproject.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.client.cache.FileResourceFactory;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.Version;
import org.callimachusproject.io.FileUtil;
import org.callimachusproject.util.MailProperties;

/**
 * Manages the connections and cache for outgoing requests.
 * 
 * @author James Leigh
 * 
 */
public class HttpClientManager implements Closeable {
	private static final String DEFAULT_NAME = Version.getInstance()
			.getVersion();
	private static HttpClientManager instance;
	static {
		try {
			File dir = File.createTempFile("http-client-cache", "");
			dir.delete();
			FileUtil.deleteOnExit(dir);
			setCacheDirectory(dir);
			instance = new HttpClientManager(dir);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				synchronized (HttpClientManager.class) {
					if (instance != null) {
						try {
							instance.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						instance = null;
					}
				}
			}
		}));
	}

	public static synchronized HttpClientManager getInstance() {
		return instance;
	}

	public static synchronized void setCacheDirectory(File dir)
			throws IOException {
		if (instance != null) {
			instance.close();
		}
		instance = new HttpClientManager(dir);
	}

	public static synchronized void invalidateCache() {
		instance.resetCache();
	}

	private final ProxyClientExecDecorator decorator;
	private final ResourceFactory entryFactory;
	private final PoolingHttpClientConnectionManager connManager;
	private final ConnectionReuseStrategy reuseStrategy;
	private final ConnectionKeepAliveStrategy keepAliveStrategy;
	private final Map<String, CloseableHttpClient> clients = new HashMap<String, CloseableHttpClient>();

	private HttpClientManager(File cacheDir) throws IOException {
		cacheDir.mkdirs();
		entryFactory = new FileResourceFactory(cacheDir);
		decorator = new ProxyClientExecDecorator();
		LayeredConnectionSocketFactory sslSocketFactory = SSLSocketFactory
				.getSystemSocketFactory();
		connManager = new PoolingHttpClientConnectionManager(RegistryBuilder
				.<ConnectionSocketFactory> create()
				.register("http", PlainSocketFactory.getSocketFactory())
				.register("https", sslSocketFactory).build());
		connManager.setDefaultSocketConfig(getDefaultSocketConfig());
		connManager.setDefaultConnectionConfig(getDefaultConnectionConfig());
		int max = Integer.parseInt(System.getProperty("http.maxConnections", "20"));
		connManager.setDefaultMaxPerRoute(max);
		connManager.setMaxTotal(2 * max);
		reuseStrategy = DefaultConnectionReuseStrategy.INSTANCE;
		keepAliveStrategy = DefaultConnectionKeepAliveStrategy.INSTANCE;
		resetCache();
	}

	public synchronized void resetCache() {
		clients.clear();
	}

	public synchronized CloseableHttpClient createHttpClient(String source) {
		final String origin = getOrigin(source);
		return new CloseableHttpClient() {
			public void close() throws IOException {
				// no-op
			}
			public HttpParams getParams() {
				return client(origin).getParams();
			}
			public ClientConnectionManager getConnectionManager() {
				return client(origin).getConnectionManager();
			}
			protected CloseableHttpResponse doExecute(HttpHost target,
					HttpRequest request, HttpContext context) throws IOException,
					ClientProtocolException {
				return client(origin).execute(target, request, context);
			}
		};
	}

	public synchronized void close() throws IOException {
		connManager.shutdown();
		for (CloseableHttpClient client : clients.values()) {
			client.close();
		}
	}

	public synchronized ClientExecChain getProxy(HttpHost destination) {
		return decorator.getProxy(destination);
	}

	public synchronized ClientExecChain setProxy(HttpHost destination,
			ClientExecChain proxy) {
		return decorator.setProxy(destination, proxy);
	}

	public synchronized boolean removeProxy(HttpHost destination,
			ClientExecChain proxy) {
		return decorator.removeProxy(destination, proxy);
	}

	public synchronized boolean removeProxy(ClientExecChain proxy) {
		return decorator.removeProxy(proxy);
	}

	synchronized CloseableHttpClient client(String origin) {
		CloseableHttpClient client = clients.get(origin);
		if (client == null) {
			ManagedHttpCacheStorage storage = new ManagedHttpCacheStorage(
					getDefaultCacheConfig());
			List<BasicHeader> headers = new ArrayList<BasicHeader>();
			headers.add(new BasicHeader("Origin", origin));
			headers.addAll(getAdditionalRequestHeaders());
			client = new AutoClosingHttpClient(new CachingHttpClientBuilder() {
				protected ClientExecChain decorateMainExec(ClientExecChain mainExec) {
					return super.decorateMainExec(decorator.decorateMainExec(mainExec));
				}
			}.setResourceFactory(entryFactory).setHttpCacheStorage(storage)
					.setConnectionManager(connManager)
					.setConnectionReuseStrategy(reuseStrategy)
					.setKeepAliveStrategy(keepAliveStrategy).useSystemProperties()
					.disableContentCompression()
					.setDefaultRequestConfig(getDefaultRequestConfig())
					.addInterceptorFirst(new GZipInterceptor())
					.addInterceptorFirst(new GUnzipInterceptor())
					.setDefaultHeaders(headers)
					.setUserAgent(DEFAULT_NAME).build(), storage);
			clients.put(origin, client);
		}
		return client;
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
				.setCircularRedirectsAllowed(false).build();
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
				.setMaxObjectSize(64000).build();
	}
}
