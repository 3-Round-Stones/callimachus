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

public class RealmManager {
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_REALM = CALLI + "Realm";
	private static final String CALLI_ORIGIN = CALLI + "Origin";
	private static volatile int cache = 0;

	public static void reset() {
		cache++;
	}

	private int revision = cache;
	private TreeMap<String, Realm> realms;

	public synchronized void resetCache() {
		realms = null;
	}

	public Realm getRealm(String target, ObjectConnection con)
			throws OpenRDFException {
		TreeMap<String, Realm> realms = getRealms(con);
		Entry<String, Realm> entry = realms.floorEntry(target);
		if (entry == null || !target.startsWith(entry.getKey()))
			return null;
		return entry.getValue();
	}

	private synchronized TreeMap<String, Realm> getRealms(
			ObjectConnection con) throws OpenRDFException {
		if (realms != null && revision == cache)
			return realms;
		revision = cache;
		return realms = loadRealms(con);
	}

	private TreeMap<String, Realm> loadRealms(ObjectConnection con)
			throws OpenRDFException {
		TreeMap<String, Realm> realms = new TreeMap<String, Realm>();
		ValueFactory vf = con.getValueFactory();
		addRealmsOfType(vf.createURI(CALLI_REALM), realms, con);
		addRealmsOfType(vf.createURI(CALLI_ORIGIN), realms, con);
		return realms;
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
					realms.put(subj.stringValue(), new Realm(subj, con));
				}
			}
		} finally {
			stmts.close();
		}
	}

}
