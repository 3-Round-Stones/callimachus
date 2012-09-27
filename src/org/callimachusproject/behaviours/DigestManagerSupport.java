package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.tools.FileObject;

import org.apache.http.HttpResponse;
import org.callimachusproject.server.auth.DigestHelper;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;

public abstract class DigestManagerSupport implements RDFObject {
	private final DigestHelper helper = new DigestHelper();

	/**
	 * Called from realm.ttl and user.ttl
	 */
	public String findCredential(Collection<String> tokens)
			throws OpenRDFException {
		return helper.findCredential(tokens, this.getResource(),
				this.getObjectConnection());
	}

	/**
	 * Called from realm.ttl
	 */
	public String findCredentialLabel(Collection<String> tokens) {
		return helper.findCredentialLabel(tokens, this.getObjectConnection());
	}

	/**
	 * Called from user.ttl
	 */
	public boolean isDigestPassword(Collection<String> tokens, String[] hash)
			throws OpenRDFException {
		return helper.isDigestPassword(tokens, hash, this.getResource(),
				this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl and user.ttl
	 */
	public Set<?> changeDigestPassword(Set<RDFObject> files, String[] passwords)
			throws RepositoryException, IOException {
		return helper.changeDigestPassword(files, passwords,
				this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl
	 */
	public HttpResponse login(Collection<String> tokens, boolean persistent)
			throws OpenRDFException {
		return helper.login(tokens, persistent, this.getResource(),
				this.getObjectConnection());
	}

	/**
	 * Called from digest.ttl
	 */
	public String getDaypass(FileObject secret) {
		return helper.getDaypass(secret);
	}

}
