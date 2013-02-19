package org.callimachusproject.setup;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class SetupOrigin implements Serializable {
	private static final long serialVersionUID = 5202002298569579860L;
	private final String root;
	private final boolean resolvable;
	private final String webappOrigin;
	private final String indexTarget;
	private final String layout;
	private final String forbiddenPage;
	private final String unauthorizedPage;
	private final String[] authentication;
	private final boolean placeholder;

	public SetupOrigin(String root, boolean resolvable, String webappOrigin) {
		assert root != null;
		assert webappOrigin != null;
		this.root = root;
		this.resolvable = resolvable;
		this.webappOrigin = webappOrigin;
		this.indexTarget = null;
		this.layout = null;
		this.forbiddenPage = null;
		this.unauthorizedPage = null;
		this.authentication = null;
		this.placeholder = true;
	}

	@ConstructorProperties({ "root", "resolvable", "webappOrigin",
			"indexTarget", "layout", "forbiddenPage", "unauthorizedPage",
			"authentication" })
	public SetupOrigin(String root, boolean resolvable, String webappOrigin,
			String indexTarget, String layout, String forbiddenPage,
			String unauthorizedPage, String[] authentication) {
		assert root != null;
		assert webappOrigin != null;
		this.root = root;
		this.resolvable = resolvable;
		this.webappOrigin = webappOrigin;
		this.indexTarget = indexTarget;
		this.layout = layout;
		this.forbiddenPage = forbiddenPage;
		this.unauthorizedPage = unauthorizedPage;
		this.authentication = authentication;
		this.placeholder = false;
	}

	public String getRoot() {
		return root;
	}

	public boolean isResolvable() {
		return resolvable;
	}

	public String getWebappOrigin() {
		return webappOrigin;
	}

	public String getIndexTarget() {
		return indexTarget;
	}

	public String getLayout() {
		return layout;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + root.hashCode();
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
		SetupOrigin other = (SetupOrigin) obj;
		if (!root.equals(other.root))
			return false;
		if (!webappOrigin.equals(other.webappOrigin))
			return false;
		return true;
	}

	protected boolean isPlaceHolder() {
		return placeholder;
	}

}
