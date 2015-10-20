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
package org.callimachusproject.sail.auditing.helpers;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.protocol.HttpContext;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class RequestActivityFactory implements ActivityFactory {
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	private static final String STARTED_AT = "http://www.w3.org/ns/prov#startedAtTime";
	private static final String ASSOC_WITH = "http://www.w3.org/ns/prov#wasAssociatedWith";
	private final URI activity;
	private final ActivityFactory delegate;
	private final DatatypeFactory df;
	private final HttpContext ctx;
	private final long receivedOn;

	public RequestActivityFactory(URI activity, ActivityFactory delegate,
			HttpContext ctx, long receivedOn) {
		this.activity = activity;
		this.delegate = delegate;
		this.ctx = ctx;
		this.receivedOn = receivedOn;
		try {
			this.df = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new AssertionError(e);
		}
	}

	public RequestActivityFactory(RequestActivityFactory copyOf,
			ActivityFactory delegate) {
		this(copyOf.activity, delegate, copyOf.ctx, copyOf.receivedOn);
	}

	@Override
	public String toString() {
		return activity.stringValue();
	}

	public URI createActivityURI(URI bundle, ValueFactory vf) {
		return activity;
	}

	@Override
	public void activityStarted(URI prov, URI graph, RepositoryConnection con)
			throws RepositoryException {
		delegate.activityStarted(prov, graph, con);
		ValueFactory vf = con.getValueFactory();
		GregorianCalendar cal = new GregorianCalendar(1970, 0, 1);
		cal.setTimeZone(UTC);
		cal.setTimeInMillis(receivedOn);
		XMLGregorianCalendar now = df.newXMLGregorianCalendar(cal);
		con.add(prov, vf.createURI(STARTED_AT), vf.createLiteral(now), graph);
		Object cred = ctx.getAttribute(AuthorizationManager.CREDENTIAL_ATTR);
		if (cred != null) {
			con.add(prov, vf.createURI(ASSOC_WITH), vf.createURI(cred.toString()), graph);
		}
	}

	public void activityEnded(URI prov, URI graph, RepositoryConnection con)
			throws RepositoryException {
		delegate.activityEnded(prov, graph, con);
	}
}
