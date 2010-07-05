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

import org.openrdf.http.object.traits.Realm;
import org.openrdf.repository.object.annotations.iri;

public interface OriginRealm extends Realm {

	/**
	 * Identifies the security contexts that caused the user agent to initiate
	 * an HTTP request.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#origin")
	Set<Object> getOrigins();

	@iri("http://callimachusproject.org/rdf/2009/framework#origin")
	void setOrigins(Set<Object> origins);

	/**
	 * Define the protection space. Any URI that has a URI in this
	 * set as a prefix (after both have been made absolute) may be assumed to
	 * be in the same protection space.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#domain")
	Set<Object> getDomains();

	@iri("http://callimachusproject.org/rdf/2009/framework#domain")
	void setDomains(Set<?> domains);
}
