package org.callimachusproject.concepts;

import org.openrdf.repository.object.annotations.iri;

/** Credentials for a person. */
@iri("http://callimachusproject.org/rdf/2009/framework#User")
public interface User {
	/** User's full name. */
	@iri("http://www.w3.org/2000/01/rdf-schema#label")
	String getCalliFullName();

	/** The primary email address for this user. */
	@iri("http://callimachusproject.org/rdf/2009/framework#email")
	String getCalliEmail();

}