package org.callimachusproject.setup;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.callimachusproject.io.CarInputStream;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.SystemProperties;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

public class CallimachusWebappImportProvider extends UpdateProvider {

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
