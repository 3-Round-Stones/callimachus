package org.callimachusproject.server.model;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.auditing.ActivityFactory;

public class RequestActivityFactory implements ActivityFactory {
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	private static final String STARTED_AT = "http://www.w3.org/ns/prov#startedAtTime";
	private static final String ENDED_AT = "http://www.w3.org/ns/prov#endedAtTime";
	private static final String ASSOC_WITH = "http://www.w3.org/ns/prov#wasAssociatedWith";
	private final URI activity;
	private final ActivityFactory delegate;
	private final DatatypeFactory df;
	private final ResourceRequest req;

	public RequestActivityFactory(URI activity, ActivityFactory delegate,
			ResourceRequest req) throws DatatypeConfigurationException {
		this.activity = activity;
		this.delegate = delegate;
		this.df = DatatypeFactory.newInstance();
		this.req = req;
	}

	public URI createActivityURI(ValueFactory vf) {
		return activity;
	}

	@Override
	public void activityStarted(URI act, RepositoryConnection con)
			throws RepositoryException {
		delegate.activityStarted(act, con);
		ValueFactory vf = con.getValueFactory();
		GregorianCalendar cal = new GregorianCalendar(1970, 0, 1);
		cal.setTimeZone(UTC);
		cal.setTimeInMillis(req.getReceivedOn());
		XMLGregorianCalendar now = df.newXMLGregorianCalendar(cal);
		con.add(act, vf.createURI(STARTED_AT), vf.createLiteral(now), act);
		String cred = req.getCredential();
		if (cred != null) {
			con.add(act, vf.createURI(ASSOC_WITH), vf.createURI(cred), act);
		}
	}

	public void activityEnded(URI act, RepositoryConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		XMLGregorianCalendar now = df
				.newXMLGregorianCalendar(new GregorianCalendar(UTC));
		con.add(act, vf.createURI(ENDED_AT), vf.createLiteral(now), act);
		delegate.activityEnded(act, con);
	}
}