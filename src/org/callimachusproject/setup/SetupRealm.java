package org.callimachusproject.setup;

import info.aduna.net.ParsedURI;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class SetupRealm implements Serializable {
	private static final long serialVersionUID = 5202002298569579860L;
	private final String realm;
	private final String webappOrigin;
	private final String layout;
	private final String errorPipe;
	private final String forbiddenPage;
	private final String unauthorizedPage;
	private final String[] authentication;
	private final String repositoryID;

	public SetupRealm(String webappOrigin, String repositoryID) {
		assert webappOrigin != null;
		this.realm = webappOrigin + "/";
		this.webappOrigin = webappOrigin;
		this.layout = null;
		this.errorPipe = null;
		this.forbiddenPage = null;
		this.unauthorizedPage = null;
		this.authentication = null;
		this.repositoryID = repositoryID;
	}

	@ConstructorProperties({ "realm", "webappOrigin", "layout", "errorPipe",
			"forbiddenPage", "unauthorizedPage", "authentication",
			"repositoryID" })
	public SetupRealm(String realm, String webappOrigin, String layout,
			String errorPipe, String forbiddenPage, String unauthorizedPage,
			String[] authentication, String repositoryID) {
		assert realm != null;
		assert webappOrigin != null;
		this.realm = realm;
		this.webappOrigin = webappOrigin;
		this.layout = layout;
		this.errorPipe = errorPipe;
		this.forbiddenPage = forbiddenPage;
		this.unauthorizedPage = unauthorizedPage;
		this.authentication = authentication;
		this.repositoryID = repositoryID;
	}

	public String getRealm() {
		return realm;
	}

	public String getOrigin() {
		ParsedURI parsed = new ParsedURI(realm);
		return parsed.getScheme() + "://" + parsed.getAuthority();
	}

	public String getWebappOrigin() {
		return webappOrigin;
	}

	public String getLayout() {
		return layout;
	}

	public String getErrorPipe() {
		return errorPipe;
	}

	public String getForbiddenPage() {
		return forbiddenPage;
	}

	public String getUnauthorizedPage() {
		return unauthorizedPage;
	}

	public String[] getAuthentication() {
		return authentication;
	}

	public String getRepositoryID() {
		return repositoryID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + realm.hashCode();
		result = prime * result
				+ webappOrigin.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SetupRealm other = (SetupRealm) obj;
		if (!realm.equals(other.realm))
			return false;
		if (!webappOrigin.equals(other.webappOrigin))
			return false;
		return true;
	}

}
