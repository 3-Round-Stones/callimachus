package org.callimachusproject.server;

import info.aduna.net.ParsedURI;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.logging.trace.TracerService;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.auditing.ActivityFactory;
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
	private static final String SLASH_ORIGIN = "/types/Origin";
	private static final String CHANGE_TYPE = "types/Change";
	private static final String FOLDER_TYPE = "types/Folder";
	private final AuditingRepository auditing;
	private final ObjectRepository object;

	public CallimachusRepository(Repository repository, File dataDir)
			throws RepositoryConfigException, RepositoryException,
			IOException {
		object = createObjectRepository(dataDir, repository);
		auditing = findAuditingRepository(repository, object);
		trace(auditing);
		setDelegate(object);
	}

	@Override
	public ObjectRepository getDelegate() {
		return object;
	}

	public void setChangeFolder(String uriSpace) throws OpenRDFException {
		if (auditing != null) {
			String bundle = getCallimachusUrl(uriSpace, CHANGE_TYPE);
			String folder = getCallimachusUrl(uriSpace, FOLDER_TYPE);
			auditing.setActivityFactory(new CallimachusActivityFactory(object,
					uriSpace, bundle, folder));
		}
	}

	public void addSchemaGraph(URI graphURI) throws RepositoryException {
		object.addSchemaGraph(graphURI);
	}

	public void addSchemaGraphType(URI rdfType) throws RepositoryException {
		object.addSchemaGraphType(rdfType);
	}

	public void setSchemaGraphType(URI rdfType) throws RepositoryException {
		object.setSchemaGraphType(rdfType);
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

	public boolean removeSchemaListener(Runnable action) {
		return object.removeSchemaListener(action);
	}

	public ObjectConnection getConnection() throws RepositoryException {
		ObjectConnection con = object.getConnection();
		if (auditing != null && con.getVersionBundle() == null) {
			URI bundle = con.getInsertContext();
			ActivityFactory activityFactory = auditing.getActivityFactory();
			if (bundle == null && activityFactory != null) {
				ValueFactory vf = getValueFactory();
				URI activityURI = activityFactory.createActivityURI(bundle, vf);
				String str = activityURI.stringValue();
				int h = str.indexOf('#');
				if (h > 0) {
					bundle = vf.createURI(str.substring(0, h));
				} else {
					bundle = activityURI;
				}
			}
			con.setVersionBundle(bundle); // use the same URI for blob version
		}
		return con;
	}

	/**
	 * Resolves the relative path to the callimachus webapp context installed at
	 * the origin.
	 * 
	 * @param origin
	 *            scheme and authority
	 * @param path
	 *            relative path from the Callimachus webapp context
	 * @return absolute URL of the root + webapp context + path (or null)
	 */
	public String getCallimachusUrl(String origin, String path)
			throws OpenRDFException {
		ParsedURI parsed = new ParsedURI(origin + "/");
		String root = parsed.getScheme() + "://" + parsed.getAuthority() + "/";
		String webapp = getCallimachusWebapp(root);
		if (webapp == null)
			return null;
		return TermFactory.newInstance(webapp).resolve(path);
	}

	/**
	 * Locates the location of the Callimachus webapp folder if present in same
	 * origin, given the root folder.
	 * 
	 * @param root
	 *            home folder, absolute URL with '/' as the path
	 * @return folder of the Callimachus webapp (or null)
	 * @throws OpenRDFException
	 */
	private String getCallimachusWebapp(String root) throws OpenRDFException {
		assert root.endsWith("/");
		RepositoryConnection con = this.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			RepositoryResult<Statement> stmts;
			stmts = con
					.getStatements(vf.createURI(root), RDF.TYPE, null, false);
			try {
				while (stmts.hasNext()) {
					String type = stmts.next().getObject().stringValue();
					if (type.startsWith(root) && type.endsWith(SLASH_ORIGIN)) {
						int end = type.length() - SLASH_ORIGIN.length();
						return type.substring(0, end + 1);
					}
				}
			} finally {
				stmts.close();
			}
		} finally {
			con.close();
		}
		return null;
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

	private ObjectRepository createObjectRepository(File dataDir,
			Repository repository) throws RepositoryConfigException,
			RepositoryException, IOException {
		if (repository instanceof ObjectRepository)
			return (ObjectRepository) repository;
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		ObjectRepositoryConfig config = factory.getConfig();
		config.setIncludeInferred(false);
		config.setObjectDataDir(dataDir);
		File wwwDir = new File(dataDir, "www");
		File blobDir = new File(dataDir, "blob");
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
