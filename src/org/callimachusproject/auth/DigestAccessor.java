package org.callimachusproject.auth;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;

public interface DigestAccessor {

	String getIdentifier();

	void registerUser(Resource invitedUser, URI registeredUser,
			ObjectConnection con) throws OpenRDFException, IOException;

	Map<String, String> findDigestUser(String username, String authName,
			Collection<String> cookies, ObjectConnection con)
			throws OpenRDFException, IOException;

	HttpResponse getLogoutResponse();

	HttpResponse getBadCredentialResponse(String method, String url,
			String[] via, Collection<String> cookies, HttpEntity body)
			throws IOException;

	HttpResponse getNotLoggedInResponse(String method, String url,
			String[] via, Collection<String> cookies, HttpEntity body)
			throws IOException;

}