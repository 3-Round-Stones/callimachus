package org.callimachusproject.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainNameSystemResolver {
	private static final DomainNameSystemResolver instance = new DomainNameSystemResolver();

	public static DomainNameSystemResolver getInstance() {
		return instance;
	}

	private final Logger logger = LoggerFactory.getLogger(DomainNameSystemResolver.class);
	private final DirContext ictx;

	private DomainNameSystemResolver() {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put("java.naming.factory.initial",
				"com.sun.jndi.dns.DnsContextFactory");
		InitialDirContext initialDirContext;
		try {
			initialDirContext = new InitialDirContext(env);
		} catch (NumberFormatException e) {
			logger.warn(e.toString(), e);
			// can't parse IPv6
			initialDirContext = null;
		} catch (NamingException e) {
			logger.warn(e.toString(), e);
			initialDirContext = null;
		}
		ictx = initialDirContext;
	}

	public String lookup(String domain, String... type) throws NamingException {
		if (ictx == null)
			return null;
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

	public InetAddress getLocalHost() {
		try {
			return InetAddress.getByName(null);
		} catch (UnknownHostException e) {
			try {
				final Enumeration<NetworkInterface> interfaces = NetworkInterface
						.getNetworkInterfaces();
				while (interfaces != null && interfaces.hasMoreElements()) {
					final Enumeration<InetAddress> addresses = interfaces
							.nextElement().getInetAddresses();
					while (addresses != null && addresses.hasMoreElements()) {
						InetAddress address = addresses.nextElement();
						if (address != null && address.isLoopbackAddress()) {
							return address;
						}
					}
				}
			} catch (SocketException se) {
			}
			throw new AssertionError("Unknown hostname: add the hostname of the machine to your /etc/hosts file.");
		}
	}

	public String getLocalHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}

	public String getCanonicalLocalHostName() {
		try {
			// attempt for the host canonical host name
			return InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
		} catch (UnknownHostException uhe) {
			try {
				// attempt to get the loop back address
				return InetAddress.getByName(null).getCanonicalHostName().toLowerCase();
			} catch (UnknownHostException uhe2) {
				// default to a standard loop back IP
				return "127.0.0.1";
			}
		}
	}

	public Collection<String> reverseAllLocalHosts() throws SocketException {
		Set<String> set = new TreeSet<String>();
		Enumeration<NetworkInterface> ifaces = NetworkInterface
				.getNetworkInterfaces();
		while (ifaces.hasMoreElements()) {
			NetworkInterface iface = ifaces.nextElement();
			Enumeration<InetAddress> raddrs = iface.getInetAddresses();
			while (raddrs.hasMoreElements()) {
				InetAddress raddr = raddrs.nextElement();
				addAllNames(raddr, set);
			}
			Enumeration<NetworkInterface> virtualIfaces = iface
					.getSubInterfaces();
			while (virtualIfaces.hasMoreElements()) {
				NetworkInterface viface = virtualIfaces.nextElement();
				Enumeration<InetAddress> vaddrs = viface.getInetAddresses();
				while (vaddrs.hasMoreElements()) {
					InetAddress vaddr = vaddrs.nextElement();
					addAllNames(vaddr, set);
				}
			}
		}
		try {
			addAllNames(InetAddress.getLocalHost(), set);
		} catch (UnknownHostException e) {
			set.add(getLocalHostName());
		}
		addAllNames(getLocalHost(), set);
		return set;
	}

	public String reverse(String ip) {
		try {
			return reverse(InetAddress.getByName(ip));
		} catch (UnknownHostException e) {
			return ip;
		}
	}

	public String reverse(InetAddress netAddr) {
		if (netAddr == null)
			return null;
		String name = netAddr.getCanonicalHostName().toLowerCase();
		try {
			if (!name.equals(netAddr.getHostAddress())
					&& netAddr.equals(InetAddress.getByName(name)))
				return name;
		} catch (UnknownHostException e) {
			// use reverse name
		}
		String address = getAddress(netAddr);
		if (address == null)
			return name;
		return address;
	}

	private void addAllNames(InetAddress addr, Set<String> set) {
		if (addr == null)
			return;
		set.add(addr.getHostAddress());
		set.add(addr.getHostName());
		set.add(addr.getCanonicalHostName());
		String address = getAddress(addr);
		if (address != null) {
			set.add(address);
		}
	}

	private String getAddress(InetAddress netAddr) {
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
		return null;
	}
}
