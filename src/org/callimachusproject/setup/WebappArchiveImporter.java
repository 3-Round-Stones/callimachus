package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.client.HttpClientManager;
import org.callimachusproject.client.UnavailableRequestDirector;
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

public class WebappArchiveImporter {
	private static final String GROUP_PUBLIC = "/auth/groups/public";
	private static final String GROUP_SUPER = "/auth/groups/super";
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
			.getLogger(WebappArchiveImporter.class);
	private final String webapp;
	private final CalliRepository repository;
	private URI[] schemaGraphs;

	public WebappArchiveImporter(String webapp, CalliRepository repository) {
		this.webapp = webapp;
		this.repository = repository;
	}

	public void setSchemaGraphs(URI... schemaGraphs) {
		this.schemaGraphs = schemaGraphs;
	}

	public void importArchive(InputStream carStream, String folder) throws IOException, OpenRDFException,
			NoSuchMethodException, InvocationTargetException {
		createFolder(folder, webapp, repository);
		importArchive(carStream, folder, webapp, repository);
	}

	protected void setFolderPermissions(URI uri, ObjectConnection con)
			throws RepositoryException {
		TermFactory tf = TermFactory.newInstance(webapp);
		add(con, uri, CALLI_READER, tf.resolve(GROUP_PUBLIC));
		add(con, uri, CALLI_ADMINISTRATOR, tf.resolve(GROUP_SUPER));
	}

	private void importArchive(InputStream carStream, String folderUri,
			String webapp, CalliRepository repository)
			throws IOException, OpenRDFException, NoSuchMethodException,
			InvocationTargetException {
		HttpHost host = URIUtils.extractHost(java.net.URI.create(webapp));
		HttpClientManager client = HttpClientManager.getInstance();
		UnavailableRequestDirector service = new UnavailableRequestDirector();
		if (client.getProxy(host) == null) {
			client.setProxy(host, service);
		}
		try {
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
				con.begin();
				if (schemaGraphs != null && schemaGraphs.length > 0) {
					con.clear(schemaGraphs);
				}
				Object folder = con.getObject(folderUri);
				Method UploadFolderComponents = findUploadFolderComponents(folder);
				try {
					logger.info("Importing {}", folderUri);
					int argc = UploadFolderComponents.getParameterTypes().length;
					Object[] args = new Object[argc];
					args[0] = carStream;
					UploadFolderComponents.invoke(folder, args);
				} catch (IllegalAccessException e) {
					throw new AssertionError(e);
				}
				repository.setCompileRepository(false);
				con.commit();
			} finally {
				con.close();
				client.removeProxy(host, service);
			}
		} finally {
			repository.setCompileRepository(false);
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

	private void createFolder(String folder, String webapp,
			CalliRepository repository) throws OpenRDFException {
		String parent = getParentFolder(folder);
		if (parent != null) {
			createFolder(parent, webapp, repository);
		}
		ValueFactory vf = repository.getValueFactory();
		ObjectConnection con = repository.getConnection();
		try {
			URI uri = vf.createURI(folder);
			if (parent == null) {
				RepositoryResult<Statement> stmts = con.getStatements(uri,
						RDF.TYPE, null);
				try {
					while (stmts.hasNext()) {
						Statement st = stmts.next();
						String type = st.getObject().stringValue();
						if (type.endsWith(ORIGIN_TYPE)
								|| type.endsWith(REALM_TYPE)
								|| type.endsWith(FOLDER_TYPE)) {
							return;
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
					return;
				if (con.hasStatement(uri, RDF.TYPE,
						vf.createURI(webapp + REALM_TYPE)))
					return;
				if (con.hasStatement(uri, RDF.TYPE,
						vf.createURI(webapp + FOLDER_TYPE)))
					return;
				if (con.hasStatement(vf.createURI(parent),
						vf.createURI(CALLI_HASCOMPONENT), uri))
					return;

				con.begin();
				con.add(vf.createURI(parent), vf.createURI(CALLI_HASCOMPONENT),
						uri);
				String label = folder.substring(parent.length())
						.replace("/", "").replace('-', ' ');
				con.add(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER));
				con.add(uri, RDF.TYPE, vf.createURI(webapp + FOLDER_TYPE));
				con.add(uri, RDFS.LABEL, vf.createLiteral(label));
				setFolderPermissions(uri, con);
				con.commit();
				return;
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

	private void add(ObjectConnection con, URI subj, String pred,
			String resource) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(subj, vf.createURI(pred), vf.createURI(resource));
	}

}
