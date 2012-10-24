package org.callimachusproject.server.auth;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import org.openrdf.repository.object.ObjectRepository;

public class AuthorizationService {
	private static final AuthorizationService instance = new AuthorizationService();

	public static AuthorizationService getInstance() {
		return instance;
	}

	private final AuthorizationFactory factory = new AuthorizationFactory();
	private final WeakHashMap<ObjectRepository, WeakReference<AuthorizationManager>> managers = new WeakHashMap<ObjectRepository, WeakReference<AuthorizationManager>>();

	public synchronized AuthorizationManager get(ObjectRepository repository) {
		AuthorizationManager manager;
		WeakReference<AuthorizationManager> ref = managers.get(repository);
		if (ref != null) {
			manager = ref.get();
			if (manager != null)
				return manager;
		}
		manager = factory.createAuthorizationManager(repository);
		ref = new WeakReference<AuthorizationManager>(manager);
		managers.put(repository, ref);
		return manager;
	}

}
