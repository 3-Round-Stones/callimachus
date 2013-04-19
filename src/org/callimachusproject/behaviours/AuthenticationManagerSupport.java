package org.callimachusproject.behaviours;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.auth.DetachedAuthenticationManager;
import org.callimachusproject.auth.DigestAccessor;
import org.callimachusproject.auth.DigestAuthenticationManager;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.AuthenticationManager;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class AuthenticationManagerSupport implements RDFObject,
		AuthenticationManager {
	private final AuthorizationService service = AuthorizationService
			.getInstance();

	@Override
	public DetachedAuthenticationManager detachAuthenticationManager(
			String path, List<String> domains, RealmManager manager)
			throws OpenRDFException, IOException {
		DigestAccessor accessor = createDigestAccessor(manager);
		if (accessor == null)
			return null;
		String authName = getCalliAuthName();
		if (authName == null) {
			authName = URI.create(accessor.getIdentifier()).getHost();
		}
		return new DigestAuthenticationManager(authName, path,
				domains, accessor);
	}

	public void resetCache() throws RepositoryException {
		ObjectConnection conn = this.getObjectConnection();
		conn.commit();
		ObjectRepository repo = conn.getRepository();
		service.get(repo).resetCache();
	}

	public boolean isProtected(String url) throws OpenRDFException, IOException {
		return getManager().isProtected(url);
	}

	/**
	 * Called from digest.ttl
	 */
	public void registerUser(Resource invitedUser, String digestUser,
			String email, String fullname) throws OpenRDFException, IOException {
		ObjectConnection con = this.getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		DigestAuthenticationManager mgr = getManager();
		mgr.registerUser(invitedUser, vf.createURI(digestUser), email, fullname, con);
	}

	/**
	 * Called from realm.ttl and user.ttl
	 */
	public String getUserIdentifier(String method, Collection<String> tokens)
			throws OpenRDFException, IOException {
		DigestAuthenticationManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUserIdentifier(method, tokens,
				this.getObjectConnection());
	}

	/**
	 * Called from realm.ttl
	 */
	public String getUserLogin(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DigestAuthenticationManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUserLogin(tokens, this.getObjectConnection());
	}

	/**
	 * called from realm.ttl
	 */
	public String[] getUsernameSetCookie(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DigestAuthenticationManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUsernameSetCookie(tokens, this.getObjectConnection());
	}

	protected DigestAuthenticationManager getManager() throws OpenRDFException,
			IOException {
		Resource self = this.getResource();
		ObjectRepository repo = this.getObjectConnection().getRepository();
		AuthorizationManager manager = service.get(repo);
		return (DigestAuthenticationManager) manager
				.getAuthenticationManager(self);
	}

	protected abstract DigestAccessor createDigestAccessor(
			RealmManager manager) throws OpenRDFException, IOException;

}
