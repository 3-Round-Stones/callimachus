package org.callimachusproject.concepts;

import java.util.Set;

import org.openrdf.http.object.traits.Realm;
import org.openrdf.repository.object.annotations.iri;

@iri("http://callimachusproject.org/rdf/2009/framework#Manifest")
public interface Manifest extends Realm {

	/**
	 * The realm used for authentication
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#authentication")
	Set<AccountManager> getCalliAuthentications();

	@iri("http://callimachusproject.org/rdf/2009/framework#authentication")
	void setCalliAuthentications(Set<? extends AccountManager> authentications);

	/**
	 * Identifies the security contexts that allow user agent scripts to
	 * initiate an HTTP request that can be seamlessly authenticated with this
	 * realm. Include '*' to allow all other contexts without agent credentials.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#origin")
	Set<Object> getCalliOrigins();

	@iri("http://callimachusproject.org/rdf/2009/framework#origin")
	void setCalliOrigins(Set<Object> origins);

	/**
	 * The RDFa template used when an agent is forbidden from the requested
	 * resource.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#forbidden")
	Page getCalliForbidden();

	@iri("http://callimachusproject.org/rdf/2009/framework#forbidden")
	void setCalliForbidden(Page forbidden);

	/**
	 * The RDFa template used when an agent is aunothorized from the requested
	 * resource.
	 */
	@iri("http://callimachusproject.org/rdf/2009/framework#unauthorized")
	Page getCalliUnauthorized();

	@iri("http://callimachusproject.org/rdf/2009/framework#unauthorized")
	void setCalliUnauthorized(Page unauthorized);
}
