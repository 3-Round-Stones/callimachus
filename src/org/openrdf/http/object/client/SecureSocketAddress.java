package org.openrdf.http.object.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SecureSocketAddress extends InetSocketAddress {
	private static final long serialVersionUID = -2111370611405188272L;

	public SecureSocketAddress(InetAddress addr, int port) {
		super(addr, port);
	}

	public SecureSocketAddress(String hostname, int port) {
		super(hostname, port);
	}

}
