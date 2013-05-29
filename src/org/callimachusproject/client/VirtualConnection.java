package org.callimachusproject.client;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.callimachusproject.util.DomainNameSystemResolver;

public class VirtualConnection implements HttpConnection, HttpInetConnection {
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
}
