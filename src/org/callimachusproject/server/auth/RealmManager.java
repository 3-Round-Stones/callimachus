package org.callimachusproject.server.auth;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;

public class RealmManager {
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_REALM = CALLI + "Realm";
	private static final String CALLI_ORIGIN = CALLI + "Origin";
	private static volatile int cache = 0;

	public static void reset() {
		cache++;
	}

	private int revision = cache;
	private final ObjectRepository repo;
	private TreeMap<String, Realm> realms;

	public RealmManager(ObjectRepository repository) {
		this.repo = repository;
	}

	public synchronized void resetCache() {
		realms = null;
	}

	public Realm getRealm(String target) throws OpenRDFException {
		TreeMap<String, Realm> realms = getRealms();
		return get(target, realms);
	}

	public AuthenticationManager getAuthenticationManager(Resource uri)
			throws OpenRDFException {
		for (Realm realm : getRealms().values()) {
			AuthenticationManager auth = realm.getAuthenticationManager(uri);
			if (auth != null)
				return auth;
		}
		return null;
	}

	private synchronized TreeMap<String, Realm> getRealms()
			throws OpenRDFException {
		if (realms != null && revision == cache)
			return realms;
		revision = cache;
		return realms = loadRealms(repo);
	}

	private TreeMap<String, Realm> loadRealms(ObjectRepository repo)
			throws OpenRDFException {
		ObjectConnection con = repo.getConnection();
		try {
			TreeMap<String, Realm> realms = new TreeMap<String, Realm>();
			ValueFactory vf = con.getValueFactory();
			addRealmsOfType(vf.createURI(CALLI_REALM), realms, con);
			addRealmsOfType(vf.createURI(CALLI_ORIGIN), realms, con);
			return realms;
		} finally {
			con.close();
		}
	}

	private void addRealmsOfType(URI type, TreeMap<String, Realm> realms,
			ObjectConnection con) throws OpenRDFException {
		RepositoryResult<Statement> stmts;
		stmts = con.getStatements(null, RDF.TYPE, type, true);
		try {
			while (stmts.hasNext()) {
				Resource subj = stmts.next().getSubject();
				if (subj instanceof URI
						&& !realms.containsKey(subj.stringValue())) {
					realms.put(subj.stringValue(), new Realm(subj, con, this));
				}
			}
		} finally {
			stmts.close();
		}
	}

	private Realm get(String target, TreeMap<String, Realm> realms) {
		Entry<String, Realm> entry = realms.floorEntry(target);
		if (entry == null)
			return null;
		String key = entry.getKey();
		if (target.startsWith(key))
			return entry.getValue();
		if (target.length() == 0)
			return null;
		int idx = 0;
		while (idx < target.length() && idx < key.length()
				&& target.charAt(idx) == key.charAt(idx)) {
			idx++;
		}
		String prefix = target.substring(0, idx);
		return get(prefix, realms);
	}

}
