package org.callimachusproject.auth;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.callimachusproject.concepts.Realm;
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
	private TreeMap<String, DetachedRealm> realms;

	public RealmManager(ObjectRepository repository) {
		this.repo = repository;
	}

	public synchronized void resetCache() {
		realms = null;
	}

	public DetachedRealm getRealm(String target) throws OpenRDFException {
		TreeMap<String, DetachedRealm> realms = getRealms();
		return get(target, realms);
	}

	public DetachedAuthenticationManager getAuthenticationManager(Resource uri)
			throws OpenRDFException {
		for (DetachedRealm realm : getRealms().values()) {
			DetachedAuthenticationManager auth = realm.getAuthenticationManager(uri);
			if (auth != null)
				return auth;
		}
		return null;
	}

	private synchronized TreeMap<String, DetachedRealm> getRealms()
			throws OpenRDFException {
		if (realms != null && revision == cache)
			return realms;
		revision = cache;
		return realms = loadRealms(repo);
	}

	private TreeMap<String, DetachedRealm> loadRealms(ObjectRepository repo)
			throws OpenRDFException {
		ObjectConnection con = repo.getConnection();
		try {
			TreeMap<String, DetachedRealm> realms = new TreeMap<String, DetachedRealm>();
			ValueFactory vf = con.getValueFactory();
			addRealmsOfType(vf.createURI(CALLI_REALM), realms, con);
			addRealmsOfType(vf.createURI(CALLI_ORIGIN), realms, con);
			return realms;
		} finally {
			con.close();
		}
	}

	private void addRealmsOfType(URI type, TreeMap<String, DetachedRealm> realms,
			ObjectConnection con) throws OpenRDFException {
		ObjectQuery qry = con.prepareObjectQuery(SPARQL, SELECT_BY_TYPE);
		qry.setBinding("type", type);
		for (Object o : qry.evaluate(Object.class).asList()) {
			String key = o.toString();
			try {
				if (!realms.containsKey(key)) {
					realms.put(key, ((Realm) o).detachRealm(this));
				}
			} catch (ClassCastException e) {
				logger.error(o.toString() + " cannot be detached", e);
			}
		}
	}

	private DetachedRealm get(String target, TreeMap<String, DetachedRealm> realms) {
		Entry<String, DetachedRealm> entry = realms.floorEntry(target);
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
