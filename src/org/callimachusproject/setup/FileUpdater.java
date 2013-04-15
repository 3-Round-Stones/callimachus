package org.callimachusproject.setup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.setup.Updater;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.store.blob.BlobObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FileUpdater implements Updater {
	private static final String READER_GROUP = "/auth/groups/public";
	private static final String SUBSCRIBER_GROUP = "/auth/groups/users";
	private static final String EDIT_GROUP[] = { "/auth/groups/staff",
			"/auth/groups/power" };
	private static final String ADMIN_GROUP = "/auth/groups/admin";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_EDITOR = CALLI + "editor";
	private static final String CALLI_SUBSCRIBER = CALLI + "subscriber";
	private static final String CALLI_READER = CALLI + "reader";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";

	private final Logger logger = LoggerFactory.getLogger(FileUpdater.class);
	private final String origin;
	private final String ifContent;

	public FileUpdater(String virtual) {
		this.origin = virtual;
		this.ifContent = null;
	}

	public FileUpdater(String origin, InputStream onlyIfContent)
			throws IOException {
		this.origin = origin;
		this.ifContent = readContent(onlyIfContent);
	}

	@Override
	public boolean update(String webapp, CalliRepository repository)
			throws IOException, OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			URI article = createFileData(origin, webapp, con);
			BlobObject blob = con.getBlobObject(article);
			CharSequence current = blob.getCharContent(true);
			if (current != null) {
				if (ifContent == null || !ifContent.equals(current.toString()))
					return false;
				logger.info("Replacing {}", article);
			}
			InputStream in = getFileResourceAsStream();
			try {
				OutputStream out = blob.openOutputStream();
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

	protected abstract InputStream getFileResourceAsStream();

	protected abstract String getFileUrl(String origin);

	protected abstract String[] getFileType(String webapps);

	protected void additionalMetadata(URI file, ObjectConnection con)
			throws RepositoryException {
	}

	private URI createFileData(String virtual, String webapp,
			ObjectConnection con) throws RepositoryException {
		TermFactory origin = TermFactory.newInstance(webapp);
		ValueFactory vf = con.getValueFactory();
		URI folder = vf.createURI(virtual + "/");
		URI file = vf.createURI(getFileUrl(virtual));
		String[] types = getFileType(webapp);
		if (con.hasStatement(file, RDF.TYPE, vf.createURI(types[0])))
			return file;
		logger.info("Uploading: {}", file);
		for (String type : types) {
			con.add(file, RDF.TYPE, vf.createURI(type));
		}
		con.add(file, RDFS.LABEL,
				vf.createLiteral(getFileLabel(file.getLocalName())));
		con.add(file, vf.createURI(CALLI_READER),
				vf.createURI(origin.resolve(READER_GROUP)));
		con.add(file, vf.createURI(CALLI_SUBSCRIBER),
				vf.createURI(origin.resolve(SUBSCRIBER_GROUP)));
		URI editor = vf.createURI(CALLI_EDITOR);
		for (String group : EDIT_GROUP) {
			con.add(file, editor, vf.createURI(origin.resolve(group)));
		}
		con.add(file, vf.createURI(CALLI_ADMINISTRATOR),
				vf.createURI(origin.resolve(ADMIN_GROUP)));
		con.add(folder, vf.createURI(CALLI_HASCOMPONENT), file);
		additionalMetadata(file, con);
		return file;
	}

	private String getFileLabel(String localName) {
		String filename = localName.replaceAll("([a-zA-Z_0-9])\\.[a-zA-Z]+$",
				"$1");
		try {
			return URLDecoder.decode(filename, "UTF-8").replaceAll(
					"[_\\-\\+\\s]+", " ");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private String readContent(InputStream in) throws IOException {
		if (in == null)
			return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ChannelUtil.transfer(in, out);
		return new String(out.toByteArray());
	}

}
