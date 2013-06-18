package org.callimachusproject.server.helpers;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

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
	private final CalliContext ctx;
	private final long receivedOn;

	public RequestActivityFactory(URI activity, ActivityFactory delegate,
			CalliContext ctx, long receivedOn) throws DatatypeConfigurationException {
		this.activity = activity;
		this.delegate = delegate;
		this.ctx = ctx;
		this.receivedOn = receivedOn;
		this.df = DatatypeFactory.newInstance();
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
		String cred = ctx.getCredential();
		if (cred != null) {
			con.add(prov, vf.createURI(ASSOC_WITH), vf.createURI(cred), graph);
		}
	}

	public void activityEnded(URI prov, URI graph, RepositoryConnection con)
			throws RepositoryException {
		delegate.activityEnded(prov, graph, con);
	}
}