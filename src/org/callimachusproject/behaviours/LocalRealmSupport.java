package org.callimachusproject.behaviours;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.repository.object.RDFObject;

public abstract class LocalRealmSupport extends RealmSupport implements RDFObject {
	private static String localhost;
	private static String[] local;
	static {
		try {
			localhost = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			localhost = "localhost";
		}
		Set<InetAddress> addresses = getAllLocalAddresses();
		if (addresses.isEmpty()) {
			local = new String[] { "dns:" + localhost };
		} else {
			local = new String[addresses.size()];
			Iterator<InetAddress> iter = addresses.iterator();
			for (int i = 0; i < local.length; i++) {
				local[i] = "dns:" + iter.next().getCanonicalHostName();
			}
		}
	}

	private static Set<InetAddress> getAllLocalAddresses() {
		Set<InetAddress> result = new HashSet<InetAddress>();
		try {
			result.addAll(Arrays.asList(InetAddress.getAllByName(null)));
		} catch (UnknownHostException e) {
			// no loop back device
		}
		try {
			InetAddress local = InetAddress.getLocalHost();
			result.add(local);
			try {
				result.addAll(Arrays.asList(InetAddress.getAllByName(local
						.getCanonicalHostName())));
			} catch (UnknownHostException e) {
				// no canonical name
			}
		} catch (UnknownHostException e) {
			// no network
		}
		try {
			Enumeration<NetworkInterface> interfaces;
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces != null && interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				Enumeration<InetAddress> addrs = iface.getInetAddresses();
				while (addrs != null && addrs.hasMoreElements()) {
					result.add(addrs.nextElement());
				}
			}
		} catch (SocketException e) {
			// broken network configuration
		}
		return result;
	}

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, String qs) {
		String via = credential.toString();
		for (String uri : local) {
			if (via.equals(uri))
				return true;
		}
		return false;
	}

}
