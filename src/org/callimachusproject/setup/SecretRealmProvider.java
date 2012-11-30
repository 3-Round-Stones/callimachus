package org.callimachusproject.setup;

import java.io.IOException;
import java.io.Writer;
import java.security.SecureRandom;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.callimachusproject.server.CallimachusRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public class SecretRealmProvider implements UpdateProvider {
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_SECRET = CALLI + "secret";

	@Override
	public String getDefaultCallimachusWebappLocation(String origin)
			throws IOException {
		return null;
	}

	@Override
	public Updater updateCallimachusWebapp(String origin) throws IOException {
		return null;
	}

	@Override
	public Updater updateOrigin(String virtual)
			throws IOException {
		return null;
	}

	@Override
	public Updater updateRealm(final String realm)
			throws IOException {
		return new Updater() {
			public boolean update(String webapp,
					CallimachusRepository repository) throws IOException,
					OpenRDFException {
				ObjectConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					ValueFactory vf = con.getValueFactory();
					URI subj = vf.createURI(realm);
					if (!con.hasStatement(subj, vf.createURI(CALLI_SECRET),
							null)) {
						URI secret = vf.createURI("urn:uuid:"
								+ UUID.randomUUID());
						con.add(subj, vf.createURI(CALLI_SECRET), secret);
						byte[] bytes = new byte[1024];
						new SecureRandom().nextBytes(bytes);
						storeTextBlob(secret, Base64.encodeBase64String(bytes),
								con);
						con.setAutoCommit(true);
						return true;
					}
					return false;
				} finally {
					con.close();
				}
			}

			private void storeTextBlob(URI uuid, String encoded,
					ObjectConnection con) throws RepositoryException,
					IOException {
				Writer writer = con.getBlobObject(uuid).openWriter();
				try {
					writer.write(encoded);
				} finally {
					writer.close();
				}
			}
		};
	}

	@Override
	public Updater updateFrom(String origin, String version) throws IOException {
		return null;
	}

}
