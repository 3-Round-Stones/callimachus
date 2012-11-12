package org.callimachusproject.auth;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.callimachusproject.traits.DetachableRealm;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectQuery;
import org.openrdf.repository.object.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmManager {
	private static final String SELECT_BY_TYPE = "SELECT ?object { ?object a $type }";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_REALM = CALLI + "Realm";
	private static final String CALLI_ORIGIN = CALLI + "Origin";
	private static volatile int cache = 0;

	public static void reset() {
		cache++;
	}

	private final Logger logger = LoggerFactory.getLogger(RealmManager.class);
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
		ObjectQuery qry = con.prepareObjectQuery(SPARQL, SELECT_BY_TYPE);
		qry.setBinding("type", type);
		for (Object o : qry.evaluate(Object.class).asList()) {
			String key = o.toString();
			try {
				if (!realms.containsKey(key)) {
					realms.put(key, ((DetachableRealm) o).detachRealm(this));
				}
			} catch (ClassCastException e) {
				logger.error(o.toString() + " cannot be detached", e);
			}
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
