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
package org.callimachusproject.server.behaviours;

import static java.lang.Integer.toHexString;

import org.callimachusproject.server.concepts.Transaction;
import org.callimachusproject.server.traits.VersionedObject;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.sail.auditing.vocabulary.Audit;

/**
 * Causes this object's revision to be increased, even if no triples are modified.
 */
public abstract class VersionedObjectSupport implements VersionedObject, RDFObject {

	public void touchRevision() {
		ObjectFactory of = getObjectConnection().getObjectFactory();
		setAuditRevision(of.createObject(Audit.CURRENT_TRX, Transaction.class));
	}

	public String revision() {
		Transaction trans = getAuditRevision();
		if (trans == null)
			return null;
		String uri = ((RDFObject) trans).getResource().stringValue();
		return toHexString(uri.hashCode());
	}

	public String revisionTag(int code) {
		String revision = revision();
		if (revision == null)
			return null;
		if (code == 0)
			return "W/" + '"' + revision + '"';
		return "W/" + '"' + revision + '-' + toHexString(code) + '"';
	}

	public String variantTag(String mediaType, int code) {
		if (mediaType == null)
			return revisionTag(code);
		String revision = revision();
		if (revision == null)
			return null;
		String cd = toHexString(code);
		String v = toHexString(mediaType.hashCode());
		if (code == 0)
			return "W/" + '"' + revision + '-' + v + '"';
		return "W/" + '"' + revision + '-' + cd + '-' + v + '"';
	}

}
