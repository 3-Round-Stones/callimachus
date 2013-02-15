package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.client.UnavailableHttpClient;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebappImportProvider extends UpdateProvider {
	private static final String GROUP_PUBLIC = "/auth/groups/public";
	private static final String GROUP_ADMIN = "/auth/groups/admin";
	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final String REALM_TYPE = "types/Realm";
	private static final String ORIGIN_TYPE = "types/Origin";
	private static final String FOLDER_TYPE = "types/Folder";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_READER = CALLI + "reader";
	private static final String CALLI_FOLDER = CALLI + "Folder";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";

	private final Logger logger = LoggerFactory
			.getLogger(WebappImportProvider.class);

	@Override
	public Updater finalizeCallimachusWebapp(final String origin)
			throws IOException {
		if (isAvailable()) {
			return new Updater() {
				public boolean update(String webapp, CalliRepository repository)
						throws IOException, OpenRDFException {
					try {
						return importCallimachusWebapp(webapp, repository);
					} catch (NoSuchMethodException e) {
						throw new UndeclaredThrowableException(e);
					} catch (InvocationTargetException e) {
						throw new UndeclaredThrowableException(e);
					}
				}
			};
		}
		return null;
	}

	protected abstract boolean isAvailable();

	protected abstract String getTargetFolder(String webapp);

	protected abstract InputStream openCarWebappStream() throws IOException;

	protected URI[] getSchemaGraphs(String folder, CalliRepository repository)
			throws IOException, OpenRDFException {
		return null;
	}

	boolean importCallimachusWebapp(String webapp, CalliRepository repository)
			throws OpenRDFException, IOException, NoSuchMethodException,
			InvocationTargetException {
		logger.info("Initializing {}", webapp);
		return importCar(getTargetFolder(webapp), webapp, repository);
	}

	private boolean importCar(String folder, String webapp,
			CalliRepository repository) throws IOException, OpenRDFException,
			NoSuchMethodException, InvocationTargetException {
		String webappOrigin = createFolder(folder, webapp, repository);
		URI[] schemaGraphs = getSchemaGraphs(folder, repository);
		importArchive(schemaGraphs, folder, webappOrigin, webapp, repository);
		return true;
	}

	private void importArchive(URI[] schemaGraphs, String folderUri,
			String origin, String webapp, CalliRepository repository)
			throws IOException, OpenRDFException, NoSuchMethodException,
			InvocationTargetException {
		HttpHost host = getAuthorityAddress(origin);
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		UnavailableHttpClient service = new UnavailableHttpClient();
		client.setProxy(host, service);
		if (schemaGraphs != null) {
			for (URI schemaGraph : schemaGraphs) {
				repository.addSchemaGraph(schemaGraph.stringValue());
			}
		} else {
			repository.setSchemaGraphType(webapp + SCHEMA_GRAPH);
		}
		repository.setCompileRepository(true);
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			if (schemaGraphs.length > 0) {
				con.clear(schemaGraphs);
			}
			Object folder = con.getObject(folderUri);
			Method UploadFolderComponents = findUploadFolderComponents(folder);
			InputStream in = openCarWebappStream();
			try {
				logger.info("Importing {}", folderUri);
				int argc = UploadFolderComponents.getParameterTypes().length;
				Object[] args = new Object[argc];
				args[0] = in;
				UploadFolderComponents.invoke(folder, args);
			} catch (IllegalAccessException e) {
				throw new AssertionError(e);
			} finally {
				in.close();
			}
			repository.setCompileRepository(false);
			con.setAutoCommit(true);
		} finally {
			con.close();
			client.removeProxy(host, service);
		}
	}

	private Method findUploadFolderComponents(Object folder)
			throws NoSuchMethodException {
		for (Method method : folder.getClass().getMethods()) {
			if ("UploadFolderComponents".equals(method.getName()))
				return method;
		}
		throw new NoSuchMethodException("UploadFolderComponents");
	}

	private String createFolder(String folder, String webapp,
			CalliRepository repository) throws OpenRDFException {
		String origin = null;
		String parent = getParentFolder(folder);
		if (parent != null) {
			origin = createFolder(parent, webapp, repository);
		}
		ValueFactory vf = repository.getValueFactory();
		ObjectConnection con = repository.getConnection();
		try {
			URI uri = vf.createURI(folder);
			if (origin == null || parent == null) {
				RepositoryResult<Statement> stmts = con.getStatements(uri,
						RDF.TYPE, null);
				try {
					while (stmts.hasNext()) {
						Statement st = stmts.next();
						String type = st.getObject().stringValue();
						if (type.endsWith(ORIGIN_TYPE)
								|| type.endsWith(REALM_TYPE)
								|| type.endsWith(FOLDER_TYPE)) {
							String root = TermFactory.newInstance(type)
									.resolve("/");
							return root.substring(0, root.length() - 1);
						}
					}
				} finally {
					stmts.close();
				}
				throw new IllegalStateException(
						"Can only import a CAR within a previously defined origin or realm");
			} else {
				if (con.hasStatement(uri, RDF.TYPE,
						vf.createURI(webapp + ORIGIN_TYPE)))
					return origin;
				if (con.hasStatement(uri, RDF.TYPE,
						vf.createURI(webapp + REALM_TYPE)))
					return origin;
				if (con.hasStatement(uri, RDF.TYPE,
						vf.createURI(webapp + FOLDER_TYPE)))
					return origin;
				if (con.hasStatement(vf.createURI(parent),
						vf.createURI(CALLI_HASCOMPONENT), uri))
					return origin;

				con.setAutoCommit(false);
				con.add(vf.createURI(parent), vf.createURI(CALLI_HASCOMPONENT),
						uri);
				String label = folder.substring(parent.length())
						.replace("/", "").replace('-', ' ');
				con.add(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER));
				con.add(uri, RDF.TYPE, vf.createURI(webapp + FOLDER_TYPE));
				con.add(uri, RDFS.LABEL, vf.createLiteral(label));
				add(con, uri, CALLI_READER, origin + GROUP_PUBLIC);
				add(con, uri, CALLI_ADMINISTRATOR, origin + GROUP_ADMIN);
				con.setAutoCommit(true);
				return origin;
			}
		} finally {
			con.close();
		}
	}

	private String getParentFolder(String folder) {
		int idx = folder.lastIndexOf('/', folder.length() - 2);
		if (idx < 0)
			return null;
		String parent = folder.substring(0, idx + 1);
		if (parent.endsWith("://"))
			return null;
		return parent;
	}

	private HttpHost getAuthorityAddress(String origin) {
		return URIUtils.extractHost(java.net.URI.create(origin + "/"));
	}

	private void add(ObjectConnection con, URI subj, String pred,
			String resource) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(subj, vf.createURI(pred), vf.createURI(resource));
	}

}