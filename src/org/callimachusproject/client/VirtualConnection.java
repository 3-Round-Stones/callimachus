package org.callimachusproject.client;

import java.io.IOException;
import java.net.InetAddress;

import javax.net.ssl.SSLSession;

import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpHost;
import org.apache.http.conn.HttpRoutedConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.callimachusproject.util.DomainNameSystemResolver;

public class VirtualConnection implements HttpRoutedConnection {
	private final HttpHost host;
	
	public VirtualConnection(HttpHost host) {
		this.host = host;
	}

	@Override
	public void shutdown() throws IOException {
		// no-op
	}
	
	@Override
	public void setSocketTimeout(int timeout) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isStale() {
		return false;
	}
	
	@Override
	public boolean isOpen() {
		return true;
	}
	
	@Override
	public int getSocketTimeout() {
		return 0;
	}
	
	@Override
	public HttpConnectionMetrics getMetrics() {
		return null;
	}
	
	@Override
	public void close() throws IOException {
		// no-op
	}
	
	@Override
	public int getRemotePort() {
		int port = host.getPort();
		if (port < 0) {
			Scheme scheme = SchemeRegistryFactory.createSystemDefault().getScheme(host);
			port = scheme.getDefaultPort();
		}
		return port;
	}
	
	@Override
	public InetAddress getRemoteAddress() {
		return DomainNameSystemResolver.getInstance().getLocalHost();
	}
	
	@Override
	public int getLocalPort() {
		return 0;
	}
	
	@Override
	public InetAddress getLocalAddress() {
		return DomainNameSystemResolver.getInstance().getLocalHost();
	}
	
	@Override
	public boolean isSecure() {
		return "https".equalsIgnoreCase(host.getSchemeName());
	}
	
	@Override
	public SSLSession getSSLSession() {
		return null;
	}
	
	@Override
	public HttpRoute getRoute() {
		return new HttpRoute(host);
	}
}
