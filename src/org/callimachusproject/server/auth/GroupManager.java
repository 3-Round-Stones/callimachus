package org.callimachusproject.server.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class GroupManager {
	private static volatile int cache = 0;

	public static void reset() {
		cache++;
	}

	private int revision = cache;
	private final Map<String, Group> groups = new HashMap<String, Group>();

	public synchronized Set<Group> getGroups(Set<String> uris, RepositoryConnection con) throws RepositoryException {
		if (revision != cache) {
			groups.clear();
			revision = cache;
		}
		Set<Group> groups = new HashSet<Group>(uris.size());
		for (String uri : uris) {
			groups.add(getGroup(uri, con));
		}
		return groups;
	}

	private synchronized Group getGroup(String uri, RepositoryConnection con) throws RepositoryException {
		if (groups.containsKey(uri))
			return groups.get(uri);
		Group group = new Group(uri, con);
		groups.put(uri, group);
		return group;
	}

}
