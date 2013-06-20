package org.callimachusproject.behaviours;

import java.io.BufferedInputStream;
import java.io.IOException;

import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.helpers.RDFHandlerBase;

public abstract class CompositeSupport implements CalliObject {

	public boolean isAuthorized(String user, RDFObject target, String[] roles)
			throws RepositoryException, OpenRDFException {
		AuthorizationManager manager = getCalliRepository().getAuthorizationManager();
		return manager.isAuthorized(user, target, roles);
	}

	public String peekAtStatementSubject(BufferedInputStream in, String type, String base)
			throws RDFParseException, IOException {
		try {
			in.mark(65536);
			RDFFormat format = RDFFormat.forMIMEType(type);
			RDFParserRegistry registry = RDFParserRegistry.getInstance();
			RDFParser parser = registry.get(format).getParser();
			parser.setRDFHandler(new RDFHandlerBase() {
				public void handleStatement(Statement st)
						throws RDFHandlerException {
					if (st.getSubject() instanceof URI) {
						throw new RDFHandlerException(st.getSubject().stringValue());
					}
				}
			});
			parser.parse(in, base);
			return null;
		} catch (RDFHandlerException e) {
			return e.getMessage();
		} finally {
			in.reset();
		}
	}

}
