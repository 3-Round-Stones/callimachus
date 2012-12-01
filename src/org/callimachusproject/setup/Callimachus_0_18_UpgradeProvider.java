package org.callimachusproject.setup;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;

import org.callimachusproject.server.CallimachusRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.ObjectConnection;

public class Callimachus_0_18_UpgradeProvider implements UpdateProvider {
	private static final String MOVED_FILE = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "SELECT DISTINCT ?file { </callimachus/> calli:hasComponent ?file FILTER (?file=</callimachus/profile> || EXISTS { ?file a foaf:Document }) }";
	private static final String MOVED_FOLDER = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "SELECT DISTINCT ?folder { </callimachus/> calli:hasComponent ?folder . ?folder a calli:Folder\n"
			+ "FILTER(?folder IN (</callimachus/types/>, </callimachus/editor/>, </callimachus/images/>, </callimachus/pages/>, </callimachus/pipelines/>, </callimachus/queries/>, </callimachus/schemas/>, </callimachus/scripts/>, </callimachus/styles/>, </callimachus/templates/>, </callimachus/theme/>, </callimachus/transforms/>)) }";

	public String getDefaultCallimachusWebappLocation(String origin)
			throws IOException {
		return null;
	}

	public Updater updateCallimachusWebapp(String origin) throws IOException {
		return null;
	}

	public Updater updateOrigin(String virtual)
			throws IOException {
		return null;
	}

	public Updater updateRealm(String realm)
			throws IOException {
		return null;
	}

	public Updater updateFrom(final String origin, String version)
			throws IOException {
		if (!"0.18".equals(version))
			return null;
		return new Updater() {
			public boolean update(String webapp,
					CallimachusRepository repository) throws IOException,
					OpenRDFException {
				boolean modified = false;
				ValueFactory vf = repository.getValueFactory();
				repository.addSchemaGraphType(vf.createURI(origin
						+ "/callimachus/SchemaGraph"));
				repository.addSchemaGraphType(vf.createURI(origin
						+ "/callimachus/types/SchemaGraph"));
				repository.addSchemaGraphType(vf.createURI(origin
						+ "/callimachus/1.0/types/SchemaGraph"));
				repository.setCompileRepository(true);
				try {
					modified |= deleteFolders(origin, webapp, repository);
					modified |= deleteFiles(origin, webapp, repository);
				} catch (IOException e) {
					throw e;
				} catch (OpenRDFException e) {
					throw e;
				} catch (Exception e) {
					throw new UndeclaredThrowableException(e);
				} finally {
					repository.setCompileRepository(false);
				}
				return modified;
			}
		};
	}

	boolean deleteFiles(String origin, String webapp, CallimachusRepository repository)
			throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			TupleQueryResult results = con.prepareTupleQuery(
					QueryLanguage.SPARQL, MOVED_FILE, webapp).evaluate();
			if (!results.hasNext())
				return false;
			try {
				while (results.hasNext()) {
					String file = results.next().getValue("file").stringValue();
					con.getBlobObject(file).delete();
					URI target = vf.createURI(file);
					con.remove(vf.createURI(origin + "/callimachus/"), null, target);
					con.remove(target, null, null);
					con.remove((Resource)null, null, null, target);
				}
			} finally {
				results.close();
			}
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
		return true;
	}

	boolean deleteFolders(String origin, String webapp,
			CallimachusRepository repository) throws Exception {
		List<String> folders = getFolders(webapp, repository);
		if (folders.isEmpty())
			return false;
		deleteFolders(folders, origin, repository);
		return true;
	}

	private List<String> getFolders(String webapp,
			CallimachusRepository repository) throws OpenRDFException {
		List<String> list = new ArrayList<String>();
		ObjectConnection con = repository.getConnection();
		try {
			TupleQueryResult results = con.prepareTupleQuery(
					QueryLanguage.SPARQL, MOVED_FOLDER, webapp).evaluate();
			try {
				while (results.hasNext()) {
					list.add(results.next().getValue("folder").stringValue());
				}
			} finally {
				results.close();
			}
		} finally {
			con.close();
		}
		return list;
	}

	private void deleteFolders(List<String> folders, String origin,
			CallimachusRepository repository) throws Exception {
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			for (String folderUri : folders) {
				Object folder = con.getObject(folderUri);
				Method DeleteComponents = findDeleteComponents(folder);
				try {
					int argc = DeleteComponents.getParameterTypes().length;
					DeleteComponents.invoke(folder, new Object[argc]);
					URI target = vf.createURI(folderUri);
					con.remove(vf.createURI(origin + "/callimachus/"), null, target);
					con.remove(target, null, null);
					con.remove((Resource)null, null, null, target);
					con.commit();
				} catch (InvocationTargetException e) {
					try {
						throw e.getCause();
					} catch (Exception cause) {
						throw cause;
					} catch (Error cause) {
						throw cause;
					} catch (Throwable cause) {
						throw e;
					}
				}
			}
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
	}

	private Method findDeleteComponents(Object folder)
			throws NoSuchMethodException {
		for (Method method : folder.getClass().getMethods()) {
			if ("DeleteComponents".equals(method.getName()))
				return method;
		}
		throw new NoSuchMethodException("DeleteComponents in " + folder);
	}

}
