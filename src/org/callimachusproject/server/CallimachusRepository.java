package org.callimachusproject.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import org.callimachusproject.logging.trace.TracerService;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.auditing.AuditingRepository;
import org.openrdf.repository.auditing.config.AuditingRepositoryFactory;
import org.openrdf.repository.base.RepositoryWrapper;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.store.blob.file.FileBlobStoreProvider;

public class CallimachusRepository extends RepositoryWrapper {
	private final AuditingRepository auditing;
	private final ObjectRepository object;

	public CallimachusRepository(Repository repository)
			throws RepositoryConfigException, RepositoryException,
			IOException {
		assert repository.getDataDir() != null;
		object = createObjectRepository(repository.getDataDir(), repository);
		auditing = findAuditingRepository(repository, object);
		trace(auditing);
		setDelegate(object);
	}

	public void setActivityFolderAndType(String uriSpace, String activityType,
			String folderType) throws DatatypeConfigurationException {
		if (auditing != null) {
			auditing.setActivityFactory(new CallimachusActivityFactory(
					uriSpace, activityType, folderType));
		}
	}

	public void addSchemaGraphType(URI rdfType) throws RepositoryException {
		object.addSchemaGraphType(rdfType);
	}

	public boolean isCompileRepository() {
		return object.isCompileRepository();
	}

	public void setCompileRepository(boolean compileRepository)
			throws ObjectStoreConfigException, RepositoryException {
		object.setCompileRepository(compileRepository);
	}

	public boolean addSchemaListener(Runnable action) {
		return object.addSchemaListener(action);
	}

	public ObjectConnection getConnection() throws RepositoryException {
		return object.getConnection();
	}

	private AuditingRepository findAuditingRepository(Repository repository,
			ObjectRepository object) throws RepositoryConfigException {
		if (repository instanceof AuditingRepository)
			return (AuditingRepository) repository;
		if (repository instanceof RepositoryWrapper)
			return findAuditingRepository(
					((RepositoryWrapper) repository).getDelegate(), object);
		Repository delegate = object.getDelegate();
		AuditingRepositoryFactory factory = new AuditingRepositoryFactory();
		AuditingRepository auditing = factory
				.getRepository(factory.getConfig());
		auditing.setDelegate(delegate);
		object.setDelegate(auditing);
		return auditing;
	}

	private void trace(RepositoryWrapper repository) {
		Repository delegate = repository.getDelegate();
		TracerService service = TracerService.newInstance();
		Repository traced = service.trace(delegate, Repository.class);
		repository.setDelegate(traced);
	}

	private ObjectRepository createObjectRepository(File dir,
			Repository repository) throws RepositoryConfigException, RepositoryException, IOException {
		if (repository instanceof ObjectRepository)
			return (ObjectRepository) repository;
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		ObjectRepositoryConfig config = factory.getConfig();
		File wwwDir = new File(dir, "www");
		File blobDir = new File(dir, "blob");
		if (wwwDir.isDirectory() && !blobDir.isDirectory()) {
			config.setBlobStore(wwwDir.toURI().toString());
			Map<String, String> map = new HashMap<String, String>();
			map.put("provider", FileBlobStoreProvider.class.getName());
			config.setBlobStoreParameters(map);
		} else {
			config.setBlobStore(blobDir.toURI().toString());
		}
		return factory.createRepository(config, repository);
	}

}
