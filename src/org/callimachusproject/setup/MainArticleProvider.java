package org.callimachusproject.setup;

import java.io.InputStream;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public class MainArticleProvider extends FileProvider {
	private static final String MAIN_ARTICLE = "META-INF/templates/main-article.docbook";
	private static final String ARTICLE_TYPE = "types/Article";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_DESCRIBEDBY = CALLI + "describedby";

	@Override
	protected InputStream getFileResourceAsStream() {
		ClassLoader cl = getClass().getClassLoader();
		return cl.getResourceAsStream(MAIN_ARTICLE);
	}

	@Override
	protected String getFileUrl(String virtual) {
		return virtual + "/main-article.docbook";
	}

	@Override
	protected String[] getFileType(String webapps) {
		return new String[] { webapps + ARTICLE_TYPE,
				"http://xmlns.com/foaf/0.1/Document" };
	}

	@Override
	protected void additionalMetadata(URI file, ObjectConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI(file.getNamespace()),
				vf.createURI(CALLI_DESCRIBEDBY),
				vf.createLiteral("main-article.docbook?view"));
	}

}
