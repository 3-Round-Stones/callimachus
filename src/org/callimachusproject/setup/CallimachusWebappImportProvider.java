package org.callimachusproject.setup;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.callimachusproject.io.CarInputStream;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.SystemProperties;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.RDFObjectException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallimachusWebappImportProvider extends UpdateProvider {
	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";

	private static final String RESOURCE_STRSTARTS = "SELECT ?resource { ?resource ?p ?o FILTER strstarts(str(?resource), str(<>)) }";
	private static final String GRAPH_STRSTARTS = "SELECT ?graph { GRAPH ?graph { ?s ?p ?o } FILTER strstarts(str(?graph), str(<>)) }";

	private final Logger logger = LoggerFactory
			.getLogger(CallimachusWebappImportProvider.class);

	@Override
	public Updater prepareCallimachusWebapp(final String origin)
			throws IOException {
		if (SystemProperties.getWebappCarFile().canRead()) {
			return new Updater() {
				public boolean update(String webapp, CalliRepository repository)
						throws IOException, OpenRDFException {
					if (!isPresent(webapp, repository))
						return false;
					logger.info("Initializing {}", origin);
					return clearCallimachusWebapp(origin, webapp, repository);
				}
			};
		}
		return null;
	}

	@Override
	public Updater updateCallimachusWebapp(final String origin)
			throws IOException {
		if (SystemProperties.getWebappCarFile().canRead()) {
			return new Updater() {
				public boolean update(String webapp, CalliRepository repository)
						throws IOException, OpenRDFException {
					try {
						importArchive(webapp, repository);
						return true;
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

	/**
	 * Remove the Callimachus webapp currently installed in <code>origin</code>.
	 * 
	 * @param origin
	 * @return <code>true</code> if the webapp was successfully removed
	 * @throws OpenRDFException
	 */
	boolean clearCallimachusWebapp(String origin, String webapp,
			CalliRepository repository) throws OpenRDFException {
		return deleteComponents(origin, webapp, repository)
				| removeAllComponents(origin, webapp, repository);
	}

	void importArchive(String webapp, CalliRepository repo) throws IOException,
			OpenRDFException, NoSuchMethodException, InvocationTargetException {
		WebappArchiveImporter importer = new WebappArchiveImporter(webapp, repo);
		importer.setSchemaGraphs(getSchemaGraphs(webapp, repo));
		InputStream in = new FileInputStream(
				SystemProperties.getWebappCarFile());
		try {
			importer.importArchive(in, webapp);
		} finally {
			in.close();
		}
	}

	private boolean isPresent(String webapp, CalliRepository repository)
			throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			return con.hasStatement(vf.createURI(webapp), null, null);
		} finally {
			con.close();
		}
	}

	private boolean deleteComponents(String origin, String webapp,
			CalliRepository repository) {
		try {
			try {
				repository.setSchemaGraphType(webapp + SCHEMA_GRAPH);
				repository.setCompileRepository(true);
				ObjectConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					RDFObject folder = (RDFObject) con.getObject(webapp);
					Method DeleteComponents = findDeleteComponents(folder);
					try {
						logger.info("Removing {}", folder);
						invokeAndRemove(DeleteComponents, folder, origin, con);
						con.setAutoCommit(true);
						return true;
					} catch (InvocationTargetException e) {
						try {
							throw e.getCause();
						} catch (Exception cause) {
							logger.warn(cause.toString());
						} catch (Error cause) {
							logger.warn(cause.toString());
						} catch (Throwable cause) {
							logger.warn(cause.toString());
						}
						con.rollback();
						return false;
					}
				} catch (IllegalAccessException e) {
					logger.debug(e.toString());
				} catch (NoSuchMethodException e) {
					logger.debug(e.toString());
				} finally {
					con.rollback();
					repository.setCompileRepository(false);
					con.close();
				}
			} finally {
				repository.setCompileRepository(false);
			}
		} catch (RDFObjectException e) {
			logger.debug(e.toString());
		} catch (OpenRDFException e) {
			logger.debug(e.toString());
		}
		return false;
	}

	private void invokeAndRemove(Method DeleteComponents, RDFObject folder,
			String origin, ObjectConnection con) throws IllegalAccessException,
			InvocationTargetException, OpenRDFException {
		int argc = DeleteComponents.getParameterTypes().length;
		DeleteComponents.invoke(folder, new Object[argc]);
		Resource target = folder.getResource();
		ValueFactory vf = con.getValueFactory();
		String parent = getParentFolder(target.stringValue());
		if (parent != null) {
			con.remove(vf.createURI(parent), null, target);
		}
		con.remove(target, null, null);
		con.remove((Resource) null, null, null, target);
	}

	private Method findDeleteComponents(Object folder)
			throws NoSuchMethodException {
		for (Method method : folder.getClass().getMethods()) {
			if ("DeleteComponents".equals(method.getName()))
				return method;
		}
		throw new NoSuchMethodException("DeleteComponents in " + folder);
	}

	private boolean removeAllComponents(String origin, String webapp,
			CalliRepository repository) throws OpenRDFException {
		boolean modified = false;
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			con.setAutoCommit(false);
			String folder = webapp;
			TupleQueryResult results;
			results = con.prepareTupleQuery(QueryLanguage.SPARQL,
					GRAPH_STRSTARTS, folder).evaluate();
			try {
				while (results.hasNext()) {
					if (!modified) {
						modified = true;
						logger.info("Expunging {}", folder);
					}
					URI graph = (URI) results.next().getValue("graph");
					con.clear(graph);
				}
			} finally {
				results.close();
			}
			results = con.prepareTupleQuery(QueryLanguage.SPARQL,
					RESOURCE_STRSTARTS, folder).evaluate();
			try {
				while (results.hasNext()) {
					if (!modified) {
						modified = true;
						logger.info("Expunging {}", folder);
					}
					URI resource = (URI) results.next().getValue("resource");
					if (folder.equals(resource.stringValue())) {
						URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
						con.remove(resource, hasComponent, null);
					} else {
						con.remove(resource, null, null);
					}
				}
			} finally {
				results.close();
			}
			con.setAutoCommit(true);
			return modified;
		} finally {
			con.rollback();
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

	private URI[] getSchemaGraphs(String folder, CalliRepository repository)
			throws IOException, OpenRDFException {
		Collection<URI> schemaGraphs = new LinkedHashSet<URI>();
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			CarInputStream carin = new CarInputStream(new FileInputStream(SystemProperties.getWebappCarFile()));
			try {
				String name;
				while ((name = carin.readEntryName()) != null) {
					try {
						URI graph = importSchemaGraphEntry(carin, folder, con);
						if (graph != null) {
							schemaGraphs.add(graph);
						}
					} catch (RDFParseException e) {
						String msg = e.getMessage() + " in " + name;
						RDFParseException pe = new RDFParseException(msg,
								e.getLineNumber(), e.getColumnNumber());
						pe.initCause(e);
						throw pe;
					}
				}
			} finally {
				carin.close();
			}
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
		return schemaGraphs.toArray(new URI[schemaGraphs.size()]);
	}

	private URI importSchemaGraphEntry(CarInputStream carin, String folder,
			ObjectConnection con) throws IOException, RDFParseException,
			RepositoryException {
		ValueFactory vf = con.getValueFactory();
		String target = folder + carin.readEntryName();
		InputStream in = carin.getEntryStream();
		try {
			if (carin.isSchemaEntry()) {
				URI graph = con.getVersionBundle();
				con.add(in, target, RDFFormat.RDFXML, graph);
				return graph;
			} else if (carin.isFileEntry()) {
				URI graph = vf.createURI(target);
				if (carin.getEntryType().startsWith("application/rdf+xml")) {
					con.clear(graph);
					con.add(in, target, RDFFormat.RDFXML, graph);
					return graph;
				} else if (carin.getEntryType().startsWith("text/turtle")) {
					con.clear(graph);
					con.add(in, target, RDFFormat.TURTLE, graph);
					return graph;
				} else {
					byte[] buf = new byte[1024];
					while (in.read(buf) >= 0)
						;
					return null;
				}
			} else {
				byte[] buf = new byte[1024];
				while (in.read(buf) >= 0)
					;
				return null;
			}
		} finally {
			in.close();
		}
	}

}
