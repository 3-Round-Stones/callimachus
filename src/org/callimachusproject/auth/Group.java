package org.callimachusproject.auth;

import java.util.Collections;
import java.util.TreeSet;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

public class Group {
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_ANONYMOUSFROM = CALLI + "anonymousFrom";
	private static final String CALLI_EVERYONEFROM = CALLI + "everyoneFrom";
	private static final String CALLI_MEMBERSFROM = CALLI + "membersFrom";
	private static final String CALLI_NOBODYFROM = CALLI + "nobodyFrom";
	private static final String CALLI_MEMBER = CALLI + "member";
	private static final Group notGroup = new Group();
	private static final Group publicGroup = new Group();
	static {
		publicGroup.anonymousFrom.add(".");
	}

	private final TreeSet<String> anonymousFrom = new TreeSet<String>();
	private final TreeSet<String> everyoneFrom = new TreeSet<String>();
	private final TreeSet<String> membersFrom = new TreeSet<String>();
	private final TreeSet<String> nobodyFrom = new TreeSet<String>();
	private final TreeSet<String> members = new TreeSet<String>();
	@SuppressWarnings("unchecked")
	private final TreeSet<String>[] sets = new TreeSet[]{anonymousFrom,
			everyoneFrom, membersFrom, nobodyFrom};

	private Group() {
		super();
	}

	Group(String uri, RepositoryConnection con) throws RepositoryException {
		URI subj = con.getValueFactory().createURI(uri);
		RepositoryResult<Statement> stmts = con.getStatements(subj, null, null, true);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				String pred = st.getPredicate().stringValue();
				if (CALLI_ANONYMOUSFROM.equals(pred)) {
					anonymousFrom.add(key(st.getObject().stringValue()));
				} else if (CALLI_EVERYONEFROM.equals(pred)) {
					everyoneFrom.add(key(st.getObject().stringValue()));
				} else if (CALLI_MEMBERSFROM.equals(pred)) {
					membersFrom.add(key(st.getObject().stringValue()));
				} else if (CALLI_NOBODYFROM.equals(pred)) {
					nobodyFrom.add(key(st.getObject().stringValue()));
				} else if (CALLI_MEMBER.equals(pred)) {
					members.add(st.getObject().stringValue());
				}
			}
		} finally {
			stmts.close();
		}
		if (this.equals(notGroup)) {
			membersFrom.add(".");
			members.add(uri);
		}
	}

	public boolean isPublic() {
		return this.equals(publicGroup);
	}

	public boolean isAnonymousAllowed(String from) {
		return isAllowed(from, anonymousFrom);
	}

	public boolean isEveryoneAllowed() {
		return !everyoneFrom.isEmpty();
	}

	public boolean isEveryoneAllowed(String from) {
		return isAllowed(from, everyoneFrom);
	}

	public boolean isMember(String user, String from) {
		return isMember(user) && isAllowed(from, membersFrom);
	}

	public boolean isMember(String user) {
		return members.contains(user);
	}

	private boolean isAllowed(String host, TreeSet<String> set) {
		String key = key(host);
		String allow = from(key, set);
		return allow != null && allow.length() >= denyFrom(key).length();
	}

	private String denyFrom(String key) {
		String deny = null;
		for (TreeSet<String> set : sets) {
			String from = from(key, set);
			if (deny == null || from != null && from.length() > deny.length()) {
				deny = from;
			}
		}
		return deny;
	}

	private String from(String key, TreeSet<String> from) {
		String floor = from.floor(key);
		if (floor != null && key.startsWith(floor))
			return floor;
		return null;
	}

	public String toString() {
		if (isPublic())
			return "Allow from all";
		StringBuilder sb = new StringBuilder();
		if (!anonymousFrom.isEmpty()) {
			sb.append("Anonymous access from ");
			if (Collections.singleton(".").equals(anonymousFrom)) {
				sb.append("all");
			} else {
				sb.append(anonymousFrom);
			}
		}
		if (!everyoneFrom.isEmpty()) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Everyone from ");
			if (Collections.singleton(".").equals(everyoneFrom)) {
				sb.append("all");
			} else {
				sb.append(everyoneFrom);
			}
		}
		if (!membersFrom.isEmpty() && !members.isEmpty()) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Allow ");
			sb.append(members);
			if (!Collections.singleton(".").equals(membersFrom)) {
				sb.append(" from ");
				sb.append(membersFrom);
			}
		}
		if (!nobodyFrom.isEmpty()) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Deny access from ");
			if (Collections.singleton(".").equals(nobodyFrom)) {
				sb.append("all");
			} else {
				sb.append(nobodyFrom);
			}
		}
		if (sb.length() == 0)
			return "Deny from all";
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + anonymousFrom.hashCode();
		result = prime * result + everyoneFrom.hashCode();
		result = prime * result + members.hashCode();
		result = prime * result + membersFrom.hashCode();
		result = prime * result + nobodyFrom.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Group other = (Group) obj;
		if (!anonymousFrom.equals(other.anonymousFrom))
			return false;
		if (!everyoneFrom.equals(other.everyoneFrom))
			return false;
		if (!members.equals(other.members))
			return false;
		if (!membersFrom.equals(other.membersFrom))
			return false;
		if (!nobodyFrom.equals(other.nobodyFrom))
			return false;
		return true;
	}

	private String key(String dns) {
		StringBuilder sb = new StringBuilder(dns.length() + 2);
		sb.append('.');
		String[] tokens = dns.split("\\.");
		for (int i=tokens.length -1 ;i>=0;i--) {
			if (tokens[i].length() > 0) {
				sb.append(tokens[i].toLowerCase());
				sb.append('.');
			}
		}
		return sb.toString();
	}
}
