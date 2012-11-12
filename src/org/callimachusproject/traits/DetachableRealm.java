package org.callimachusproject.traits;

import org.callimachusproject.auth.Realm;
import org.callimachusproject.auth.RealmManager;
import org.openrdf.OpenRDFException;

public interface DetachableRealm {

	Realm detachRealm(RealmManager manager) throws OpenRDFException;
}
