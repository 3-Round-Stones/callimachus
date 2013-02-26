package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServeAllProvider extends UpdateProvider {
	private static final String SERVICEABLE = "types/Serviceable";
	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final String GRAPH_DOCUMENT = "types/GraphDocument";
	private static final String SERVE_ALL = "../everything-else-public.ttl";
	private static final String SERVE_ALL_TTL = "META-INF/templates/callimachus-all-serviceable.ttl";
	private static final String GROUP_ADMIN = "/auth/groups/admin";
	private static final String GROUP_STAFF = "/auth/groups/staff";
	private static final String GROUP_PUBLIC = "/auth/groups/public";

	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_REALM = CALLI + "Realm";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_SUBSCRIBER = CALLI + "subscriber";
	private static final String CALLI_READER = CALLI + "reader";

	final Logger logger = LoggerFactory.getLogger(ServeAllProvider.class);

	@Override
	public Updater updateCallimachusWebapp(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				TermFactory tf = TermFactory.newInstance(webapp);
				ObjectConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					boolean modified = false;
					ValueFactory vf = con.getValueFactory();
					String single = getSingleOrigin(con);
					if ((origin+"/").equals(single)) {
						URI file = vf.createURI(tf.resolve(SERVE_ALL));
						modified |= stopServingOther(con, file);
						modified |= serverAs(tf, con, file);
					}
					con.setAutoCommit(true);
					return modified;
				} finally {
					con.close();
				}
			}
		};
	}

	@Override
	public Updater finalizeCallimachusWebapp(String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				ObjectConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					boolean modified = false;
					String single = getSingleOrigin(con);
					if (single == null) {
						modified |= stopServingOther(con, null);
					}
					con.setAutoCommit(true);
					return modified;
				} finally {
					con.close();
				}
			}
		};
	}

	boolean stopServingOther(ObjectConnection con, URI file)
			throws RepositoryException {
		boolean modified = false;
		ValueFactory vf = con.getValueFactory();
		URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
		URI NamedGraph = vf.createURI("http://www.w3.org/ns/sparql-service-description#NamedGraph");
		RepositoryResult<Statement> stmts = con.getStatements(null, RDF.TYPE, NamedGraph);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				Resource serve = st.getSubject();
				if (serve.stringValue().matches(".*" + SERVE_ALL) && !serve.equals(file)) {
					logger.info("Other resources are no longer served publicly through {}", st.getSubject());
					con.clear(serve);
					con.remove((Resource) null, hasComponent, serve);
					modified = true;
				}
			}
		} finally {
			stmts.close();
		}
		return modified;
	}

	boolean serverAs(TermFactory tf, ObjectConnection con, URI file)
			throws IOException, RepositoryException, RDFParseException {
		boolean modified = false;
		ValueFactory vf = con.getValueFactory();
		URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
		URI NamedGraph = vf.createURI("http://www.w3.org/ns/sparql-service-description#NamedGraph");
		ClassLoader cl = getClass().getClassLoader();
		InputStream in = cl.getResourceAsStream(SERVE_ALL_TTL);
		try {
			if (file != null && in != null) {
				String content = new Scanner(in).useDelimiter("\\A").next();
				String SchemaGraph = tf.resolve(SCHEMA_GRAPH);
				String Serviceable = tf.resolve(SERVICEABLE);
				content = content.replace("$SchemaGraph", SchemaGraph);
				content = content.replace("$Serviceable", Serviceable);
				logger.info("All other resources are now served publicly through {}", tf.resolve("/"));
				Writer out = con.getBlobObject(file).openWriter();
				try {
					out.write(content);
				} finally {
					out.close();
					in.close();
					in = con.getBlobObject(file).openInputStream();
				}
				con.clear(file);
				con.add(in, file.stringValue(), RDFFormat.TURTLE, file);
				if (!con.hasStatement(vf.createURI(tf.resolve("../")), hasComponent, file)) {
					con.add(file, RDFS.LABEL, vf.createLiteral("everything else public"));
					con.add(file, RDF.TYPE, NamedGraph);
					con.add(file, RDF.TYPE, vf.createURI(tf.resolve(GRAPH_DOCUMENT)));
					con.add(file, RDF.TYPE, vf.createURI("http://xmlns.com/foaf/0.1/Document"));
					con.add(file, vf.createURI(CALLI_READER), vf.createURI(tf.resolve(GROUP_PUBLIC)));
					con.add(file, vf.createURI(CALLI_SUBSCRIBER), vf.createURI(tf.resolve(GROUP_STAFF)));
					con.add(file, vf.createURI(CALLI_ADMINISTRATOR), vf.createURI(tf.resolve(GROUP_ADMIN)));
					con.add(vf.createURI(tf.resolve("../")), hasComponent, file);
				}
				modified = true;
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return modified;
	}

	String getSingleOrigin(ObjectConnection con) throws RepositoryException {
		Set<String> set = new HashSet<String>();
		ValueFactory vf = con.getValueFactory();
		Resource nil = (Resource) null;
		URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
		for (Statement st : con.getStatements(nil, RDF.TYPE,
				vf.createURI(CALLI_REALM)).asList()) {
			if (!con.hasStatement(nil, hasComponent, st.getSubject())) {
				set.add(st.getSubject().stringValue());
			}
		}
		if (set.size() == 1)
			return set.iterator().next();
		return null;
	}

}
