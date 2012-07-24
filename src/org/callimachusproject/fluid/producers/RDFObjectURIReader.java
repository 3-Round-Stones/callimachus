/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.callimachusproject.fluid.producers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.producers.base.URIListReader;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Reads RDFObjects from a URI list.
 * 
 * @author James Leigh
 * 
 */
public class RDFObjectURIReader extends URIListReader<Object> {

	public RDFObjectURIReader() {
		super(null);
	}

	public boolean isReadable(FluidType mtype, ObjectConnection con) {
		if (!super.isReadable(mtype, con))
			return false;
		Class<?> c = mtype.getClassType();
		if (mtype.isSetOrArray()) {
			c = mtype.getComponentClass();
		}
		if (Object.class.equals(c))
			return true;
		if (RDFObject.class.isAssignableFrom(c))
			return true;
		return isConcept(c, con);
	}

	@Override
	protected Object create(ObjectConnection con, String uri)
			throws MalformedURLException, RepositoryException {
		if (uri == null)
			return null;
		if (uri.startsWith("_:"))
			return con.getObject(con.getValueFactory().createBNode(
					uri.substring(2)));
		return con.getObject(uri);
	}

	private boolean isConcept(Class<?> component, ObjectConnection con) {
		for (Annotation ann : component.getAnnotations()) {
			for (Method m : ann.annotationType().getDeclaredMethods()) {
				if (m.isAnnotationPresent(Iri.class))
					return true;
			}
		}
		return con.getObjectFactory().isNamedConcept(component);
	}

}
