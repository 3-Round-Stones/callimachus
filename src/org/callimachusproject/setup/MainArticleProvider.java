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

public class MainArticleProvider implements UpdateProvider {
	private static final String MAIN_ARTICLE = "META-INF/templates/main-article.docbook";
	private static final String ARTICLE_TYPE = "types/Article";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_EDITOR = CALLI + "editor";
	private static final String CALLI_SUBSCRIBER = CALLI + "subscriber";
	private static final String CALLI_READER = CALLI + "reader";
	private static final String CALLI_DESCRIBEDBY = CALLI + "describedby";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private final Logger logger = LoggerFactory
			.getLogger(MainArticleProvider.class);

	@Override
	public String getDefaultCallimachusWebappLocation(String origin)
			throws IOException {
		return null;
	}

	@Override
	public Updater initialize(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CallimachusRepository repository)
					throws IOException, OpenRDFException {
				ClassLoader cl = getClass().getClassLoader();
				ObjectConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					URI article = createArticleData(origin, webapp, con);
					InputStream in = cl.getResourceAsStream(MAIN_ARTICLE);
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

			private URI createArticleData(final String origin, String webapp,
					ObjectConnection con) throws RepositoryException {
				ValueFactory vf = con.getValueFactory();
				URI folder = vf.createURI(origin + "/");
				URI article = vf.createURI(origin + "/main-article.docbook");
				logger.info("Uploading main article: {}", article);
				con.add(article, RDF.TYPE, vf.createURI(webapp + ARTICLE_TYPE));
				con.add(article, RDF.TYPE,
						vf.createURI("http://xmlns.com/foaf/0.1/Document"));
				con.add(article, RDFS.LABEL, vf.createLiteral("main article"));
				con.add(article, vf.createURI(CALLI_READER),
						vf.createURI(origin + "/group/public"));
				con.add(article, vf.createURI(CALLI_SUBSCRIBER),
						vf.createURI(origin + "/group/users"));
				con.add(article, vf.createURI(CALLI_EDITOR),
						vf.createURI(origin + "/group/staff"));
				con.add(article, vf.createURI(CALLI_ADMINISTRATOR),
						vf.createURI(origin + "/group/admin"));
				con.add(folder, vf.createURI(CALLI_HASCOMPONENT), article);
				con.add(folder, vf.createURI(CALLI_DESCRIBEDBY),
						vf.createLiteral("main-article.docbook?view"));
				return article;
			}
		};
	}

	@Override
	public Updater updateFrom(String origin, String version) throws IOException {
		return null;
	}

	@Override
	public Updater update(String origin) throws IOException {
		return null;
	}

}
