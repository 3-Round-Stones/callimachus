package org.callimachusproject.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class DomainNameSystemResolver {
	private static final DomainNameSystemResolver instance;
	static {
		try {
			instance = new DomainNameSystemResolver();
		} catch (NamingException e) {
			throw new AssertionError(e);
		}
	}

	public static DomainNameSystemResolver getInstance() {
		return instance;
	}

	private final DirContext ictx;

	private DomainNameSystemResolver() throws NamingException {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put("java.naming.factory.initial",
				"com.sun.jndi.dns.DnsContextFactory");
		ictx = new InitialDirContext(env);
	}

	public String lookup(String domain, String... type) throws NamingException {
		Attributes attrs = ictx.getAttributes(domain, type);
		Enumeration e = attrs.getAll();
		if (e.hasMoreElements()) {
			Attribute a = (Attribute) e.nextElement();
			int size = a.size();
			if (size > 0) {
				return (String) a.get(0);
			}
		}
		return null;
	}

	public Collection<String> reverseAllLocalHosts() throws SocketException {
		Collection<String> set = new TreeSet<String>();
		Enumeration<NetworkInterface> ifaces = NetworkInterface
				.getNetworkInterfaces();
		while (ifaces.hasMoreElements()) {
			NetworkInterface iface = ifaces.nextElement();
			Enumeration<InetAddress> raddrs = iface.getInetAddresses();
			while (raddrs.hasMoreElements()) {
				InetAddress raddr = raddrs.nextElement();
				set.add(reverse(raddr));
			}
			Enumeration<NetworkInterface> virtualIfaces = iface
					.getSubInterfaces();
			while (virtualIfaces.hasMoreElements()) {
				NetworkInterface viface = virtualIfaces.nextElement();
				Enumeration<InetAddress> vaddrs = viface.getInetAddresses();
				while (vaddrs.hasMoreElements()) {
					InetAddress vaddr = vaddrs.nextElement();
					set.add(reverse(vaddr));
				}
			}
		}
		try {
			InetAddress local = InetAddress.getLocalHost();
			set.add(reverse(local));
		} catch (UnknownHostException e) {
			throw new AssertionError(e);
		}
		return set;
	}

	public String reverseLocalHost() {
		try {
			return reverse(InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			throw new AssertionError(e);
		}
	}

	public String reverse(String ip) {
		try {
			return reverse(InetAddress.getByName(ip));
		} catch (UnknownHostException e) {
			return ip;
		}
	}

	public String reverse(InetAddress netAddr) {
		String name = netAddr.getCanonicalHostName();
		try {
			if (!name.equals(netAddr.getHostAddress())
					&& netAddr.equals(InetAddress.getByName(name)))
				return name;
		} catch (UnknownHostException e) {
			// use reverse name
		}
		byte[] addr = netAddr.getAddress();
		if (addr.length == 4) { // IPv4 Address
			StringBuilder sb = new StringBuilder();
			for (int i = addr.length - 1; i >= 0; i--) {
				sb.append((addr[i] & 0xff) + ".");
			}
			return sb.append("in-addr.arpa").toString();
		} else if (addr.length == 16) { // IPv6 Address
			StringBuilder sb = new StringBuilder();
			for (int i = addr.length - 1; i >= 0; i--) {
				sb.append(Integer.toHexString((addr[i] & 0x0f)));
				sb.append(".");
				sb.append(Integer.toHexString((addr[i] & 0xf0) >> 4));
				sb.append(".");
			}
			return sb.append("ip6.arpa").toString();
		}
		return name;
	}
}
