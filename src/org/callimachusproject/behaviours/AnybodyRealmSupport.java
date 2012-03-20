/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.behaviours;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.callimachusproject.server.traits.Realm;
import org.openrdf.annotations.Sparql;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

/**
 * Permits any client in this realm and records the external host name.
 * 
 * @author James Leigh
 * 
 */
public abstract class AnybodyRealmSupport implements Realm, RDFObject {

	private static Set<String> local;
	static {
		Set<InetAddress> addresses = getAllLocalAddresses();
		local = new HashSet<String>(addresses.size() * 3);
		for (InetAddress addr : addresses) {
			local.add(addr.getCanonicalHostName());
			local.add(addr.getHostAddress());
			local.add(addr.getHostName());
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

	public boolean authorizeCredential(Object credential, String method,
			Object resource, Map<String, String[]> map) {
		return true;
	}

	@Sparql("SELECT (group_concat(?origin;separator=' ') as ?domain)\n"
			+ "WHERE { ?origin a </callimachus/Origin> }")
	public abstract String protectionDomain();

	@Override
	public final String allowOrigin() {
		return "*";
	}

	@Override
	public final boolean withAgentCredentials(String origin) {
		return true;
	}

	@Override
	public final HttpResponse forbidden(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		return null;
	}

	@Override
	public final Object authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException {
		String[] vias = request.get("via");
		if (vias == null || vias.length < 1)
			return null;
		String host = findRemoteHost(vias);
		if (host == null)
			return null;
		ObjectFactory of = getObjectConnection().getObjectFactory();
		return of.createObject("dns:" + host);

	}

	@Override
	public final HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request) {
		return null;
	}

	@Override
	public final HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		return null;
	}

	/**
	 * Finds the nearest remote agent or (if no remote hosts) the farthest local
	 * agent.
	 * 
	 * @return the host and optional port number or a pseudonym
	 */
	private String findRemoteHost(String[] vias) {
		String via = null;
		for (int i = vias.length - 1; i >= 0; i--) {
			via = vias[i];
			int idx = via.lastIndexOf(' ');
			if (idx > 0 && idx < via.length() - 1) {
				via = via.substring(idx + 1);
				idx = via.lastIndexOf(' ');
				if (idx > 0 && idx < via.length()) {
					via = via.substring(0, idx);
				}
			}
			if (!isLocal(via))
				break;
		}
		assert via != null;
		return via;
	}

	/**
	 * 
	 * @param via
	 *            host with optional port number
	 * @return true if this host resolves to the local machine
	 */
	private boolean isLocal(String via) {
		int idx = via.lastIndexOf(':');
		if (idx > 0 && local.contains(via.substring(0, idx)))
			return true;
		return local.contains(via);
	}

}

