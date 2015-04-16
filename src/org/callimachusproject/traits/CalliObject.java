/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.traits;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFFormat;

/**
 * An interface that exposes the auditing SAIL's revision and provides access to
 * CalliRepository and HttpClient.
 */
public interface CalliObject extends RDFObject {

	@Iri("http://www.w3.org/ns/prov#wasGeneratedBy")
	Activity getProvWasGeneratedBy();

	@Iri("http://www.w3.org/ns/prov#wasGeneratedBy")
	void setProvWasGeneratedBy(Activity activity);

	CalliRepository getCalliRepository() throws OpenRDFException, IOException;

	DetachedRealm getRealm() throws OpenRDFException, IOException;

	HttpUriClient getHttpClient() throws OpenRDFException, IOException;

	void touchRevision() throws RepositoryException;

	String revision();

	void resetAllCache();

	Model getSchemaModel();

	void setSchemaGraph(URI graph, GraphQueryResult schema) throws OpenRDFException, IOException;

	void setSchemaGraph(URI graph, Reader reader, RDFFormat format) throws OpenRDFException, IOException;

	void setSchemaGraph(URI graph, InputStream stream, RDFFormat format) throws OpenRDFException, IOException;

}
