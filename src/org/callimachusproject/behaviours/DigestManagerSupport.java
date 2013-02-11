package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;

import org.apache.http.HttpResponse;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.auth.DetachedAuthenticationManager;
import org.callimachusproject.auth.DetachedDigestManager;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.DigestManager;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class DigestManagerSupport implements RDFObject, DigestManager {
	private final AuthorizationService service = AuthorizationService.getInstance();

	@Override
	public DetachedAuthenticationManager detachAuthenticationManager(
			String path, List<String> domains, RealmManager manager)
			throws OpenRDFException {
		Resource subj = this.getResource();
		return new DetachedDigestManager(subj, getAuthName(), path, domains, manager);
	}

	public void resetCache() throws RepositoryException {
		ObjectConnection conn = this.getObjectConnection();
        conn.commit();
		ObjectRepository repo = conn.getRepository();
		service.get(repo).resetCache();
	}

	/**
	 * Called from realm.ttl and user.ttl
	 */
	public String getUserIdentifier(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedDigestManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUserIdentifier(tokens, this.getObjectConnection());
	}

	/**
	 * Called from realm.ttl
	 */
	public String getUserLogin(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedDigestManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUserLogin(tokens, this.getObjectConnection());
	}

	/**
	 * called from realm.ttl
	 */
	@Override
	public String[] getUsernameSetCookie(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedDigestManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getUsernameSetCookie(tokens, this.getObjectConnection());
	}

	/**
	 * Called from user.ttl
	 */
	public boolean isDigestPassword(Collection<String> tokens, String[] hash)
			throws OpenRDFException, IOException {
		DetachedDigestManager digest = getManager();
		if (digest == null)
			return false;
		return digest
				.isDigestPassword(tokens, hash, this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl and user.ttl
	 */
	public Set<?> changeDigestPassword(Set<RDFObject> files, String[] passwords)
			throws OpenRDFException, IOException {
		DetachedDigestManager digest = getManager();
		if (digest == null)
			return null;
		return digest.changeDigestPassword(files, passwords,
				this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl
	 */
	public HttpResponse login(Collection<String> tokens, boolean persistent)
			throws OpenRDFException, IOException {
		DetachedDigestManager digest = getManager();
		if (digest == null)
			return null;
		return digest.login(tokens, persistent, this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl
	 */
	public String getDaypass(FileObject secret) throws OpenRDFException, IOException {
		DetachedDigestManager digest = getManager();
		if (digest == null)
			return null;
		return digest.getDaypass(secret);
	}

	private DetachedDigestManager getManager() throws OpenRDFException, IOException {
		Resource self = this.getResource();
		ObjectRepository repo = this.getObjectConnection().getRepository();
		AuthorizationManager manager = service.get(repo);
		return (DetachedDigestManager) manager.getAuthenticationManager(self);
	}

}
