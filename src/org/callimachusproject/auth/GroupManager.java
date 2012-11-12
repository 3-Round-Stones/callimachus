package org.callimachusproject.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectRepository;

public class GroupManager {
	private static volatile int cache = 0;

	public static void reset() {
		cache++;
	}

	private int revision = cache;
	private final Repository repo;
	private final Map<String, Group> groups = new HashMap<String, Group>();

	public GroupManager(ObjectRepository repository) {
		this.repo = repository;
	}

	public synchronized void resetCache() {
		groups.clear();
	}

	public synchronized Set<Group> getGroups(Set<String> uris) throws RepositoryException {
		if (revision != cache) {
			resetCache();
			revision = cache;
		}
		Set<Group> groups = new HashSet<Group>(uris.size());
		for (String uri : uris) {
			groups.add(getGroup(uri));
		}
		return groups;
	}

	private synchronized Group getGroup(String uri) throws RepositoryException {
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
