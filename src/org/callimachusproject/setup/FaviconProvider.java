package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.callimachusproject.server.CallimachusRepository;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaviconProvider implements UpdateProvider {
	private static final String FAVICON = "META-INF/templates/callimachus-icon.ico";
	private static final String ICO_TYPE = "types/IconGraphic";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_EDITOR = CALLI + "editor";
	private static final String CALLI_READER = CALLI + "reader";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private final Logger logger = LoggerFactory
			.getLogger(FaviconProvider.class);

	@Override
	public String getDefaultCallimachusWebappLocation(String origin)
			throws IOException {
		return null;
	}

	@Override
	public Updater initialize(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp,
					CallimachusRepository repository) throws IOException,
					OpenRDFException {
				ClassLoader cl = getClass().getClassLoader();
				ObjectConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					URI icon = createIconData(origin, webapp, con);
					if (icon == null) {
						con.setAutoCommit(true);
						return false;
					}
					InputStream in = cl.getResourceAsStream(FAVICON);
					try {
						OutputStream out = con.getBlobObject(icon)
								.openOutputStream();
						try {
							ChannelUtil.transfer(in, out);
						} finally {
							out.close();
						}
					} finally {
						in.close();
					}
					con.setAutoCommit(true);
				} finally {
					con.close();
				}
				return true;
			}

			private URI createIconData(final String origin, String webapp,
					ObjectConnection con) throws RepositoryException {
				ValueFactory vf = con.getValueFactory();
				URI folder = vf.createURI(origin + "/");
				URI icon = vf.createURI(origin + "/favicon.ico");
				URI has = vf.createURI(CALLI_HASCOMPONENT);
				if (con.hasStatement(folder, has, icon))
					return null;
				logger.info("Uploading icon: {}", icon);
				con.add(icon, RDF.TYPE, vf.createURI(webapp + ICO_TYPE));
				con.add(icon, RDF.TYPE,
						vf.createURI("http://xmlns.com/foaf/0.1/Image"));
				con.add(icon, RDFS.LABEL, vf.createLiteral("favicon"));
				con.add(icon, vf.createURI(CALLI_READER),
						vf.createURI(origin + "/group/public"));
				con.add(icon, vf.createURI(CALLI_EDITOR),
						vf.createURI(origin + "/group/staff"));
				con.add(icon, vf.createURI(CALLI_ADMINISTRATOR),
						vf.createURI(origin + "/group/admin"));
				con.add(folder, has, icon);
				return icon;
			}
		};
	}

	@Override
	public Updater updateFrom(String origin, String version) throws IOException {
		if ("0.18".equals(version))
			return initialize(origin);
		return null;
	}

	@Override
	public Updater update(String origin) throws IOException {
		return null;
	}

}
