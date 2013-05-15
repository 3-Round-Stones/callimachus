/*
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
package org.callimachusproject.repository.auditing.helpers;

import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.callimachusproject.repository.auditing.ActivityFactory;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * Generates a unique local identifier with the provided namespace for every
 * activity. This class assigns the prov:endedAtTime when the activity ends.
 * 
 * @author James Leigh
 * 
 */
public class ActivitySequenceFactory implements ActivityFactory {
	private static final String FRAGMENT = "#activity";
	public static final URI ENDED_AT = new URIImpl(
			"http://www.w3.org/ns/prov#endedAtTime");
	private final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private final AtomicLong seq = new AtomicLong(0);
	private final DatatypeFactory df;
	private final String ns;

	public ActivitySequenceFactory(String ns) {
		this.ns = ns;
		df = createDatatypeFactory();
	}

	public URI createActivityURI(URI bundle, ValueFactory vf) {
		if (bundle == null)
			return vf.createURI(ns + uid + seq.getAndIncrement()
					+ FRAGMENT);
		return vf.createURI(bundle.stringValue() + FRAGMENT);
	}

	public void activityStarted(URI activity, URI bundle,
			RepositoryConnection con) throws RepositoryException {
		// do nothing
	}

	public void activityEnded(URI activity, URI bundle, RepositoryConnection con)
			throws RepositoryException {
		if (df != null) {
			ValueFactory vf = con.getValueFactory();
			XMLGregorianCalendar now = df
					.newXMLGregorianCalendar(new GregorianCalendar());
			con.add(activity, ENDED_AT, vf.createLiteral(now), bundle);
		}
	}

	private DatatypeFactory createDatatypeFactory() {
		try {
			return DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			return null;
		}
	}

}
