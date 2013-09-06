package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.List;

import javax.tools.FileObject;

import org.callimachusproject.auth.CookieAuthenticationManager;
import org.callimachusproject.auth.DetachedAuthenticationManager;
import org.callimachusproject.auth.FacebookAuthReader;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.FacebookManager;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.RDFObject;

public abstract class FacebookManagerSupport extends
		AuthenticationManagerSupport implements RDFObject, FacebookManager {

	@Override
	public DetachedAuthenticationManager detachAuthenticationManager(
			String path, List<String> domains, RealmManager manager)
			throws OpenRDFException, IOException {
		String uri = this.getResource().stringValue();
		String url = uri + "?login";
		String appId = getCalliFacebookAppId();
		FileObject secretFile = getCalliFacebookSecret();
		if (secretFile == null)
			return null;
		CharSequence secret = secretFile.getCharContent(false);
		return new CookieAuthenticationManager(uri, url, path, domains,
				manager, new FacebookAuthReader(uri, appId, secret, getHttpClient()));
	}

}
