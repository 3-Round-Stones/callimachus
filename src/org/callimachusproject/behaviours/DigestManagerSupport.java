package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.auth.DigestAuthenticationManager;
import org.callimachusproject.auth.DigestPasswordAccessor;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.concepts.DigestManager;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

public abstract class DigestManagerSupport extends AuthenticationManagerSupport
		implements RDFObject, DigestManager {
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");

	private final AuthorizationService service = AuthorizationService
			.getInstance();

	/**
	 * Called from digest.ttl
	 */
	public HttpResponse getPersistentLogin(String method,
			Collection<String> tokens) throws OpenRDFException, IOException {
		DigestAuthenticationManager digest = getManager();
		if (digest == null)
			return null;
		ObjectConnection con = this.getObjectConnection();
		String iri = digest.getUserIdentifier(method, tokens, con);
		DigestPasswordAccessor accessor = (DigestPasswordAccessor) digest
				.getDigestAccessor();
		HttpResponse resp = accessor.getPersistentLogin(iri);
		resp.addHeader("Cache-Control", "no-cache");
		String[] cookies = digest.getUsernameSetCookie(tokens, con);
		for (String cookie : cookies) {
			resp.addHeader("Set-Cookie", cookie);
		}
		return resp;
	}

	/**
	 * Called from digest.ttl
	 */
	public HttpResponse getLogin(String method,
			Collection<String> tokens) throws OpenRDFException, IOException {
		DigestAuthenticationManager digest = getManager();
		if (digest == null)
			return null;
		ObjectConnection con = this.getObjectConnection();
		HttpResponse resp = new BasicHttpResponse(_204);
		resp.addHeader("Cache-Control", "no-cache");
		String[] cookies = digest.getUsernameSetCookie(tokens, con);
		for (String cookie : cookies) {
			resp.addHeader("Set-Cookie", cookie);
		}
		return resp;
	}

	/**
	 * Called from digest.ttl
	 */
	public String getDaypass(RDFObject obj) throws OpenRDFException,
			IOException {
		DigestPasswordAccessor digest = getAccessor();
		if (digest == null)
			return null;
		ObjectRepository repo = obj.getObjectConnection().getRepository();
		AuthorizationManager manager = service.get(repo);
		String uri = obj.getResource().stringValue();
		String secret = manager.getRealm(uri).getOriginSecret();
		return digest.getDaypass(secret);
	}

	/**
	 * Called from user.ttl
	 */
	public boolean isDigestPassword(Collection<String> tokens, String[] hash)
			throws OpenRDFException, IOException {
		DigestAuthenticationManager digest = getManager();
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
		DigestPasswordAccessor digest = getAccessor();
		if (digest == null)
			return null;
		ObjectConnection con = this.getObjectConnection();
		String webapp = CalliRepository.getCallimachusWebapp(this.toString(),
				con);
		return digest.changeDigestPassword(files, passwords, webapp, con);
	}

	protected DigestPasswordAccessor createDigestAccessor(RealmManager manager) {
		Resource self = this.getResource();
		return new DigestPasswordAccessor(self, manager);
	}

	private DigestPasswordAccessor getAccessor() throws OpenRDFException,
			IOException {
		return (DigestPasswordAccessor) getManager().getDigestAccessor();
	}

}
