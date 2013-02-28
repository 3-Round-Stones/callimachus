package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public class MainArticleProvider extends UpdateProvider {
	private static final String MAIN_ARTICLE = "META-INF/templates/main-article.docbook";
	private static final String ARTICLE_TYPE = "types/Article";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_DESCRIBEDBY = CALLI + "describedby";

	@Override
	public Updater updateCallimachusWebapp(String origin) throws IOException {
		return new FileUpdater(origin) {

			@Override
			protected String getFileUrl(String origin) {
				return origin + "/main-article.docbook";
			}

			@Override
			protected String[] getFileType(String webapps) {
				return new String[] { webapps + ARTICLE_TYPE,
						"http://xmlns.com/foaf/0.1/Document" };
			}

			@Override
			protected InputStream getFileResourceAsStream() {
				ClassLoader cl = getClass().getClassLoader();
				return cl.getResourceAsStream(MAIN_ARTICLE);
			}

			@Override
			protected void additionalMetadata(URI file, ObjectConnection con)
					throws RepositoryException {
				ValueFactory vf = con.getValueFactory();
				URI folder = vf.createURI(file.getNamespace());
				URI describedby = vf.createURI(CALLI_DESCRIBEDBY);
				if (!con.hasStatement(folder, describedby, null)) {
					con.add(folder, describedby,
							vf.createLiteral("main-article.docbook?view"));
				}
			}
		};
	}

}
