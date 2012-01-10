package org.openrdf.http.object.behaviours;

import java.io.IOException;
import java.io.InputStream;

import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.concepts.HTTPFileObject;
import org.openrdf.http.object.exceptions.MethodNotAllowed;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;


public abstract class TextFile implements HTTPFileObject, RDFObject {
	@method("GET")
	@type("text/plain")
	public InputStream getInputStream() throws IOException {
		return openInputStream();
	}

	@query({})
	@method("DELETE")
	public void deleteObject() throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		con.clear(getResource());
		con.removeDesignations(this, vf.createURI("urn:mimetype:text/plain"));
		if (!delete())
			throw new MethodNotAllowed();
	}
}
