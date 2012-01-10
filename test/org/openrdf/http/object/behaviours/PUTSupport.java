package org.openrdf.http.object.behaviours;

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.concepts.Alias;
import org.openrdf.http.object.concepts.HTTPFileObject;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.BehaviourException;

public abstract class PUTSupport implements HTTPFileObject, RDFObject {

	@query( {})
	@method("PUT")
	public void putIntputStream(@header("Content-Location") String location,
			@header("Content-Type") String mediaType, @type("*/*") InputStream in)
			throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		if (location == null) {
			try {
				OutputStream out = openOutputStream();
				try {
					int read;
					byte[] buf = new byte[1024];
					while ((read = in.read(buf)) >= 0) {
						out.write(buf, 0, read);
					}
				} finally {
					out.close();
					in.close();
				}
				if (mediaType == null) {
					setInternalMediaType("application/octet-stream");
				} else {
					setInternalMediaType(mediaType);
				}
			} catch (IOException e) {
				throw new BadRequest(e);
			}
		} else {
			Alias alias = con.addDesignation(this, Alias.class);
			ParsedURI base = new ParsedURI(getResource().stringValue());
			ParsedURI to = base.resolve(location);
			alias.setRedirectsTo((HTTPFileObject) con.getObject(to.toString()));
		}
	}

	private void setInternalMediaType(String mediaType) {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		try {
			URI uri = vf.createURI("urn:mimetype:" + mimeType(mediaType));
			con.addDesignations(this, uri);
		} catch (RepositoryException e) {
			throw new BehaviourException(e);
		}
	}

	private String mimeType(String media) {
		if (media == null)
			return null;
		int idx = media.indexOf(';');
		if (idx > 0)
			return media.substring(0, idx);
		return media;
	}
}
