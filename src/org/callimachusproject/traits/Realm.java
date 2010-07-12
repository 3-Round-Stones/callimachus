/*
 * Copyright (c) 2009-2010, James Leigh, Some rights reserved.
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

import java.util.Set;

import org.callimachusproject.concepts.Template;
import org.openrdf.repository.object.annotations.iri;

public interface Realm extends org.openrdf.http.object.traits.Realm {

	/**
	 * Define the protection space for this realm to authenticate. Any URI that
	 * has a URI in this set as a prefix may be assumed to be in the same
	 * protection space.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#domain")
	Set<Object> getCalliDomains();

	@iri("http://callimachusproject.org/rdf/2009/framework#domain")
	void setCalliDomains(Set<?> domains);

	/**
	 * Identifies the security contexts that allow user agent scripts to
	 * initiate an HTTP request that can be seamlessly authenticated with this
	 * realm or '*' for all contexts.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#origin")
	Set<Object> getCalliOrigins();

	@iri("http://callimachusproject.org/rdf/2009/framework#origin")
	void setCalliOrigins(Set<Object> origins);

	/**
	 * Identifies the security contexts that allow agent scripts to initiate an
	 * HTTP request with this realm or '*' for all contexts."
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#script")
	Set<Object> getCalliScripts();

	@iri("http://callimachusproject.org/rdf/2009/framework#script")
	void setCalliScripts(Set<Object> scripts);

	/**
	 * The RDFa template used when an agent is forbidden from the requested
	 * resource.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#forbidden")
	Template getCalliForbidden();

	@iri("http://callimachusproject.org/rdf/2009/framework#forbidden")
	void setCalliForbidden(Template forbidden);

	/**
	 * The RDFa template used when an agent is aunothorized from the requested
	 * resource.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#unauthorized")
	Template getCalliUnauthorized();

	@iri("http://callimachusproject.org/rdf/2009/framework#unauthorized")
	void setCalliUnauthorized(Template unauthorized);
}
