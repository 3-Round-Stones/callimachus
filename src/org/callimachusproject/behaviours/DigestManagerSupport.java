package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.tools.FileObject;

import org.apache.http.HttpResponse;
import org.callimachusproject.server.auth.AuthorizationManager;
import org.callimachusproject.server.auth.AuthorizationService;
import org.callimachusproject.server.auth.DigestManager;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class DigestManagerSupport implements RDFObject {
	private final AuthorizationService service = AuthorizationService.getInstance();

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
		DigestManager digest = getDigest();
		if (digest == null)
			return null;
		return digest.findCredential(tokens, this.getObjectConnection());
	}

	/**
	 * Called from realm.ttl
	 */
	public String findCredentialLabel(Collection<String> tokens)
			throws OpenRDFException {
		DigestManager digest = getDigest();
		if (digest == null)
			return null;
		return digest.findCredentialLabel(tokens, this.getObjectConnection());
	}

	/**
	 * Called from user.ttl
	 */
	public boolean isDigestPassword(Collection<String> tokens, String[] hash)
			throws OpenRDFException {
		DigestManager digest = getDigest();
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
		DigestManager digest = getDigest();
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
		DigestManager digest = getDigest();
		if (digest == null)
			return null;
		return digest.login(tokens, persistent, this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl
	 */
	public String getDaypass(FileObject secret) throws OpenRDFException {
		DigestManager digest = getDigest();
		if (digest == null)
			return null;
		return digest.getDaypass(secret);
	}

	private DigestManager getDigest() throws OpenRDFException {
		Resource self = this.getResource();
		ObjectRepository repo = this.getObjectConnection().getRepository();
		AuthorizationManager manager = service.get(repo);
		return (DigestManager) manager.getAuthenticationManager(self);
	}

}
