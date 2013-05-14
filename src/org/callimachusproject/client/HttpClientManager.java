package org.callimachusproject.client;

import static org.apache.http.client.params.ClientPNames.ALLOW_CIRCULAR_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.ClientPNames.DEFAULT_HEADERS;
import static org.apache.http.client.params.ClientPNames.HANDLE_AUTHENTICATION;
import static org.apache.http.client.params.ClientPNames.HANDLE_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.MAX_REDIRECTS;
import static org.apache.http.client.params.CookiePolicy.IGNORE_COOKIES;
import static org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.SOCKET_BUFFER_SIZE;
import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.STALE_CONNECTION_CHECK;
import static org.apache.http.params.CoreConnectionPNames.TCP_NODELAY;
import static org.apache.http.params.CoreProtocolPNames.USER_AGENT;
import static org.apache.http.params.CoreProtocolPNames.USE_EXPECT_CONTINUE;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.FileResourceFactory;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.callimachusproject.Version;
import org.callimachusproject.io.FileUtil;
import org.callimachusproject.util.MailProperties;

/**
 * Manages the connections and cache for outgoing requests.
 * 
 * @author James Leigh
 * 
 */
public class HttpClientManager {
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
						instance.shutdown();
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
			instance.shutdown();
		}
		instance = new HttpClientManager(dir);
	}

	public static synchronized void invalidateCache() {
		instance.resetCache();
	}

	private final InternalHttpClient internal;
	private final ResourceFactory entryFactory;
	private HttpCacheClient client;
	private int numberOfClientCalls = 0;

	private HttpClientManager(File dir) throws IOException {
		dir.mkdirs();
		entryFactory = new FileResourceFactory(dir);
		HttpParams params = getDefaultHttpParams();
		SystemDefaultHttpClient client = new SystemDefaultHttpClient(params);
		client.addRequestInterceptor(new GZipInterceptor());
		internal = new InternalHttpClient(client);
		resetCache();
	}

	public synchronized void resetCache() {
		CacheConfig config = getDefaultCacheConfig();
		this.client = new HttpCacheClient(internal, entryFactory, config);
	}

	public synchronized HttpClient getClient() {
		if (++numberOfClientCalls % 100 == 0) {
			// Deletes the (no longer used) temporary cache files from disk.
			client.cleanResources();
		}
		return client;
	}

	public synchronized void shutdown() {
		internal.getConnectionManager().shutdown();
		client.shutdown();
	}

	public synchronized RequestDirector getProxy(HttpHost destination) {
		return internal.getProxy(destination);
	}

	public synchronized RequestDirector setProxy(HttpHost destination,
			RequestDirector proxy) {
		return internal.setProxy(destination, proxy);
	}

	public synchronized boolean removeProxy(HttpHost destination,
			RequestDirector proxy) {
		return internal.removeProxy(destination, proxy);
	}

	public synchronized boolean removeProxy(RequestDirector proxy) {
		return internal.removeProxy(proxy);
	}

	private HttpParams getDefaultHttpParams() throws IOException {
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(SO_TIMEOUT, 0);
		params.setIntParameter(CONNECTION_TIMEOUT, 10000);
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024);
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false);
		params.setBooleanParameter(TCP_NODELAY, false);
		params.setBooleanParameter(USE_EXPECT_CONTINUE, true);
		params.setParameter(USER_AGENT, DEFAULT_NAME);
		params.setParameter(COOKIE_POLICY, IGNORE_COOKIES);
		params.setBooleanParameter(HANDLE_AUTHENTICATION, false);
		params.setBooleanParameter(HANDLE_REDIRECTS, false);
		params.setIntParameter(MAX_REDIRECTS, 20);
		params.setBooleanParameter(ALLOW_CIRCULAR_REDIRECTS, false);
		try {
			MailProperties mail = MailProperties.getInstance();
			String from = mail.getMailProperties().get("mail.from");
			if (from != null) {
				BasicHeader hd = new BasicHeader("From", from);
				params.setParameter(DEFAULT_HEADERS, Collections.singleton(hd));
			}
		} catch (SecurityException e) {
			// ignore
		}
		return params;
	}

	private CacheConfig getDefaultCacheConfig() {
		int n = Runtime.getRuntime().availableProcessors();
		CacheConfig config = new CacheConfig();
		config.setSharedCache(true);
		config.setAsynchronousWorkersCore(1);
		config.setAsynchronousWorkersMax(n);
		config.setHeuristicCachingEnabled(true);
		config.setHeuristicDefaultLifetime(60 * 60 * 24);
		config.setMaxObjectSize(64000);
		return config;
	}
}
