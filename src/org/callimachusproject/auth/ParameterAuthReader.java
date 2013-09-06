package org.callimachusproject.auth;

import java.io.IOException;

import org.apache.http.HttpEntity;

public interface ParameterAuthReader {
	String getLoginPage(String returnTo, boolean loggedIn, String parameters, String[] via);

	String getParameters(String method, String uri, String query, HttpEntity body);

	boolean isLoggingIn(String parameters);

	boolean isCanncelled(String parameters);

	boolean isValidParameters(String parameters, String[] via) throws IOException;

	String getUserIdentifier(String parameters);

	String getUserFullName(String parameters);

	String getUserLogin(String parameters);
}
