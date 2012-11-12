package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;

import org.apache.http.HttpResponse;
import org.callimachusproject.auth.AuthenticationManager;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.auth.DigestManagerImpl;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.DigestManager;
import org.callimachusproject.traits.DetachableAuthenticationManager;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class DigestManagerSupport implements RDFObject, DigestManager, DetachableAuthenticationManager {
	private final AuthorizationService service = AuthorizationService.getInstance();

	@Override
	public AuthenticationManager detachAuthenticationManager(
			String path, List<String> domains, RealmManager manager)
			throws OpenRDFException {
		Resource subj = this.getResource();
		return new DigestManagerImpl(subj, getAuthName(), path, domains, manager);
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
	public String findCredential(Collection<String> tokens)
			throws OpenRDFException {
		DigestManagerImpl digest = getDigest();
		if (digest == null)
			return null;
		return digest.findCredential(tokens, this.getObjectConnection());
	}

	/**
	 * Called from realm.ttl
	 */
	public String findCredentialLabel(Collection<String> tokens)
			throws OpenRDFException {
		DigestManagerImpl digest = getDigest();
		if (digest == null)
			return null;
		return digest.findCredentialLabel(tokens, this.getObjectConnection());
	}

	/**
	 * Called from user.ttl
	 */
	public boolean isDigestPassword(Collection<String> tokens, String[] hash)
			throws OpenRDFException {
		DigestManagerImpl digest = getDigest();
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
		DigestManagerImpl digest = getDigest();
		if (digest == null)
			return null;
		return digest.changeDigestPassword(files, passwords,
				this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl
	 */
	public HttpResponse login(Collection<String> tokens, boolean persistent)
			throws OpenRDFException {
		DigestManagerImpl digest = getDigest();
		if (digest == null)
			return null;
		return digest.login(tokens, persistent, this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl
	 */
	public String getDaypass(FileObject secret) throws OpenRDFException {
		DigestManagerImpl digest = getDigest();
		if (digest == null)
			return null;
		return digest.getDaypass(secret);
	}

	private DigestManagerImpl getDigest() throws OpenRDFException {
		Resource self = this.getResource();
		ObjectRepository repo = this.getObjectConnection().getRepository();
		AuthorizationManager manager = service.get(repo);
		return (DigestManagerImpl) manager.getAuthenticationManager(self);
	}

}
