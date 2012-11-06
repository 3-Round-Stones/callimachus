/*
 * Copyright (c) 2009, James Leigh, Some rights reserved.
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.behaviours;

import static java.lang.Integer.toHexString;

import org.callimachusproject.concepts.Activity;
import org.callimachusproject.server.CallimachusActivityFactory;
import org.callimachusproject.traits.VersionedObject;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

/**
 * Causes this object's revision to be increased, even if no triples are modified.
 */
public abstract class VersionedObjectSupport implements VersionedObject, RDFObject {

	public void touchRevision() {
		Resource resource = getResource();
		if (resource instanceof URI) {
			String self = resource.stringValue();
			ObjectFactory of = getObjectConnection().getObjectFactory();
			if (self.contains("#")) {
				VersionedObject parent = (VersionedObject) of.createObject(self.substring(0, self.indexOf('#')));
				parent.touchRevision();
			} else {
				URI activityURI = getObjectConnection().getActivityURI();
				if (activityURI == null) {
					setProvWasGeneratedBy(null);
				} else {
					String uri = activityURI.stringValue() + CallimachusActivityFactory.PROV_SUFFIX;
					setProvWasGeneratedBy(of.createObject(uri, Activity.class));
				}
			}
		}
	}

	public String revision() {
		try {
			Activity activity = getProvWasGeneratedBy();
			if (activity == null)
				return null;
			String uri = ((RDFObject) activity).getResource().stringValue();
			return toHexString(uri.hashCode());
		} catch (ClassCastException e) {
			return null;
		}
	}

}
