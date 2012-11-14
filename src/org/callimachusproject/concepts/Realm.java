package org.callimachusproject.concepts;

import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.auth.RealmManager;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;

@Iri("http://callimachusproject.org/rdf/2009/framework#Realm")
public interface Realm {

	DetachedRealm detachRealm(RealmManager manager) throws OpenRDFException;
}
