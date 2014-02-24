/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.server.behaviours;

import java.io.IOException;
import java.io.InputStream;

import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.concepts.HTTPFileObject;
import org.callimachusproject.server.exceptions.MethodNotAllowed;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;


public abstract class TextFile implements HTTPFileObject, RDFObject {
	@method("GET")
	@type("text/plain")
	@requires("urn:test:grant")
	public InputStream getInputStream() throws IOException {
		return openInputStream();
	}

	@query({})
	@method("DELETE")
	@requires("urn:test:grant")
	public void deleteObject() throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		con.clear(getResource());
		con.removeDesignations(this, vf.createURI("urn:mimetype:text/plain"));
		if (!delete())
			throw new MethodNotAllowed();
	}
}
