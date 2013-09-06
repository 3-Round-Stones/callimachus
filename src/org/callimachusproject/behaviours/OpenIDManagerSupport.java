package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.List;

import org.callimachusproject.auth.CookieAuthenticationManager;
import org.callimachusproject.auth.DetachedAuthenticationManager;
import org.callimachusproject.auth.OpenIDAuthReader;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.OpenIDManager;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.RDFObject;

public abstract class OpenIDManagerSupport extends AuthenticationManagerSupport
		implements RDFObject, OpenIDManager {

	@Override
	public DetachedAuthenticationManager detachAuthenticationManager(
			String path, List<String> domains, RealmManager manager)
			throws OpenRDFException, IOException {
		String uri = this.getResource().stringValue();
		String url = uri + "?login";
		String realm = getOpenIdRealm();
		if (realm == null || realm.length() == 0) {
			realm = manager.getRealm(uri).getResource().stringValue();
		}
		return new CookieAuthenticationManager(uri, url, path, domains,
				manager, new OpenIDAuthReader(uri, getOpenIdEndpointUrl(),
						realm, getHttpClient()));
	}

}
