package org.callimachusproject.traits;

public interface SelfAuthorizingTarget {
	boolean calliIsAuthorized(Object credential, String method, String query);
}
