package org.callimachusproject.traits;

import java.util.List;

import org.callimachusproject.auth.AuthenticationManager;
import org.callimachusproject.auth.RealmManager;
import org.openrdf.OpenRDFException;

public interface DetachableAuthenticationManager {

	AuthenticationManager detachAuthenticationManager(String path,
			List<String> domains, RealmManager manager) throws OpenRDFException;
}
