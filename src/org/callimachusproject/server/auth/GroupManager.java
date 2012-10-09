package org.callimachusproject.server.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class GroupManager {
	private static volatile int cache = 0;

	public static void reset() {
		cache++;
	}

	private int revision = cache;
	private final Map<String, Group> groups = new HashMap<String, Group>();

	public synchronized void resetCache() {
		groups.clear();
	}

	public synchronized Set<Group> getGroups(Set<String> uris, Repository repo) throws RepositoryException {
		if (revision != cache) {
			resetCache();
			revision = cache;
		}
		Set<Group> groups = new HashSet<Group>(uris.size());
		for (String uri : uris) {
			groups.add(getGroup(uri, repo));
		}
		return groups;
	}

	private synchronized Group getGroup(String uri, Repository repo) throws RepositoryException {
		if (groups.containsKey(uri))
			return groups.get(uri);
		RepositoryConnection con = repo.getConnection();
		try {
			Group group = new Group(uri, con);
			groups.put(uri, group);
			return group;
		} finally {
			con.close();
		}
	}

}
