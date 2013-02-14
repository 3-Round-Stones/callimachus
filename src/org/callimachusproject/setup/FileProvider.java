package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FileProvider extends UpdateProvider {
	private static final String READER_GROUP = "/auth/groups/public";
	private static final String SUBSCRIBER_GROUP = "/auth/groups/users";
	private static final String EDIT_GROUP = "/auth/groups/staff";
	private static final String ADMIN_GROUP = "/auth/groups/admin";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_EDITOR = CALLI + "editor";
	private static final String CALLI_SUBSCRIBER = CALLI + "subscriber";
	private static final String CALLI_READER = CALLI + "reader";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private final Logger logger = LoggerFactory
			.getLogger(FileProvider.class);

	@Override
	public Updater updateOrigin(final String virtual) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				ObjectConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					URI article = createArticleData(virtual, webapp, con);
					if (article == null)
						return false;
					InputStream in = getFileResourceAsStream();
					try {
						OutputStream out = con.getBlobObject(article)
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

			private URI createArticleData(String virtual, String webapp,
					ObjectConnection con) throws RepositoryException {
				TermFactory origin = TermFactory.newInstance(webapp);
				ValueFactory vf = con.getValueFactory();
				URI folder = vf.createURI(virtual + "/");
				URI file = vf.createURI(getFileUrl(virtual));
				String[] types = getFileType(webapp);
				if (con.hasStatement(file, RDF.TYPE, vf.createURI(types[0])))
					return null;
				logger.info("Uploading: {}", file);
				for (String type : types) {
					con.add(file, RDF.TYPE, vf.createURI(type));
				}
				con.add(file, RDFS.LABEL, vf.createLiteral(getFileLabel(file.getLocalName())));
				con.add(file, vf.createURI(CALLI_READER),
						vf.createURI(origin.resolve(READER_GROUP)));
				con.add(file, vf.createURI(CALLI_SUBSCRIBER),
						vf.createURI(origin.resolve(SUBSCRIBER_GROUP)));
				con.add(file, vf.createURI(CALLI_EDITOR),
						vf.createURI(origin.resolve(EDIT_GROUP)));
				con.add(file, vf.createURI(CALLI_ADMINISTRATOR),
						vf.createURI(origin.resolve(ADMIN_GROUP)));
				con.add(folder, vf.createURI(CALLI_HASCOMPONENT), file);
				additionalMetadata(file, con);
				return file;
			}
		};
	}

	protected abstract InputStream getFileResourceAsStream();

	protected abstract String getFileUrl(String virtual);

	protected abstract String[] getFileType(String webapps);

	protected String getFileLabel(String localName) {
		String filename = localName.replaceAll("([a-zA-Z_0-9])\\.[a-zA-Z]+$",
				"$1");
		try {
			return URLDecoder.decode(filename, "UTF-8").replaceAll(
					"[_\\-\\+\\s]+", " ");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	protected void additionalMetadata(URI file, ObjectConnection con)
			throws RepositoryException {
		// nothing
	}

}
