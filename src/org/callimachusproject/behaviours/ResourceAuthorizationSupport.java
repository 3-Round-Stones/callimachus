package org.callimachusproject.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.InputStream;
import java.util.Scanner;

import org.callimachusproject.traits.SelfAuthorizingTarget;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ResourceAuthorizationSupport implements SelfAuthorizingTarget,
		RDFObject {
	private static final String AUTHORIZED_RESOURCE_RQ = "org/callimachusproject/xsl/authorized-resource.rq";
	private static final String AUTHORIZED;
	static {
		ClassLoader cl = ResourceAuthorizationSupport.class.getClassLoader();
		InputStream in = cl.getResourceAsStream(AUTHORIZED_RESOURCE_RQ);
		AUTHORIZED = new Scanner(in, "UTF-8").useDelimiter("\\A").next();
	}

	private Logger logger = LoggerFactory.getLogger(ResourceAuthorizationSupport.class);

	@Override
	public boolean calliIsAuthorized(Object credential, String method,
			String query) {
		if (this.getResource().stringValue().equals(credential.toString()))
			return true;
		ObjectConnection con = getObjectConnection();
		try {
			ObjectFactory of = con.getObjectFactory();
			BooleanQuery qry = con.prepareBooleanQuery(SPARQL, AUTHORIZED);
			qry.setBinding("this", getResource());
			qry.setBinding("credential", of.createValue(credential));
			qry.setBinding("method", of.createLiteral(method));
			if (query != null) {
				qry.setBinding("query", of.createLiteral(query));
			}
			return qry.evaluate();
		} catch (QueryEvaluationException e) {
			logger.error(e.toString(), e);
		} catch (MalformedQueryException e) {
			logger.error(e.toString(), e);
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
		}
		return false;
	}

}
