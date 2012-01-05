package org.callimachusproject.concepts;

import org.openrdf.annotations.Iri;

/** Credentials for a person. */
@Iri("http://callimachusproject.org/rdf/2009/framework#User")
public interface User {
	/** User's full name. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#label")
	String getCalliFullName();

	/** The primary email address for this user. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#email")
	String getCalliEmail();

}