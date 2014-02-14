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

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.concepts.Alias;
import org.callimachusproject.server.concepts.HTTPFileObject;
import org.callimachusproject.server.exceptions.BadRequest;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.BehaviourException;

public abstract class PUTSupport implements HTTPFileObject, RDFObject {

	@query( {})
	@method("PUT")
	@requires("urn:test:grant")
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
