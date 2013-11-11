package org.callimachusproject.client;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.execchain.ClientExecChain;

public class ProxyClientExecDecorator {
	private final ConcurrentMap<HttpHost, ClientExecChain> proxies = new ConcurrentHashMap<HttpHost, ClientExecChain>();

	public ClientExecChain getProxy(HttpHost host) {
		return proxies.get(host);
	}

	public ClientExecChain setProxy(HttpHost host, ClientExecChain proxy) {
		if (proxy == null) {
			return proxies.remove(key(host));
		} else {
			return proxies.put(key(host), proxy);
		}
	}

	public ClientExecChain setProxyIfAbsent(HttpHost host, ClientExecChain proxy) {
		assert proxy != null;
		return proxies.putIfAbsent(key(host), proxy);
	}

	public boolean removeProxy(HttpHost host, ClientExecChain proxy) {
		return proxies.remove(key(host), proxy);
	}

	public boolean removeProxy(ClientExecChain proxy) {
		return proxies.values().removeAll(Collections.singleton(proxy));
	}

	public ClientExecChain decorateMainExec(final ClientExecChain mainExec) {
		return new ClientExecChain() {
			public CloseableHttpResponse execute(HttpRoute route,
					HttpRequestWrapper request, HttpClientContext context,
					HttpExecutionAware execAware) throws IOException,
					HttpException {
				HttpHost host = route.getTargetHost();
				if (host != null) {
					ClientExecChain proxy = proxies.get(host);
					if (proxy != null) {
						ClientExecChain execChain = new AuthenticationClientExecChain(proxy);
						return execChain.execute(route, request, context,
								execAware);
					}
				}
				return mainExec.execute(route, request, context, execAware);
			}
		};
	}

	private HttpHost key(HttpHost host) {
		if (host != null && host.getPort() < 0) {
			try {
				int port = DefaultSchemePortResolver.INSTANCE.resolve(host);
				host = new HttpHost(host.getHostName(), port, host.getSchemeName());
			} catch (UnsupportedSchemeException e) {
				throw new AssertionError(e);
			}
		}
		return host;
	}

}
