package org.callimachusproject.concepts;

import java.util.Set;

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
	/** The primary email address for this user */
	@Iri("http://callimachusproject.org/rdf/2009/framework#email")
	void setCalliEmail(CharSequence calliEmail);

	/** The username for this agent */
	@Iri("http://callimachusproject.org/rdf/2009/framework#name")
	CharSequence getCalliName();
	/** The username for this agent */
	@Iri("http://callimachusproject.org/rdf/2009/framework#name")
	void setCalliName(CharSequence calliName);

	/** A document of the MD5 sum of email:authName:password in HEX encoding */
	@Iri("http://callimachusproject.org/rdf/2009/framework#passwordDigest")
	Set<Object> getCalliPasswordDigest();
	/** A document of the MD5 sum of email:authName:password in HEX encoding */
	@Iri("http://callimachusproject.org/rdf/2009/framework#passwordDigest")
	void setCalliPasswordDigest(Set<?> calliPasswordDigest);

}