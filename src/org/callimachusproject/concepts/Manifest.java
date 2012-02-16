package org.callimachusproject.concepts;

import java.util.Set;

import org.callimachusproject.server.traits.Realm;
import org.openrdf.annotations.Iri;

@Iri("http://callimachusproject.org/rdf/2009/framework#Realm")
public interface Manifest extends Realm {

	/**
	 * The realm used for authentication
	 */
	@Iri("http://callimachusproject.org/rdf/2009/framework#authentication")
	Set<AccountManager> getCalliAuthentications();

	@Iri("http://callimachusproject.org/rdf/2009/framework#authentication")
	void setCalliAuthentications(Set<? extends AccountManager> authentications);

	/**
	 * The RDFa template used when an agent is forbidden from the requested
	 * resource.
	 */
	@Iri("http://callimachusproject.org/rdf/2009/framework#forbidden")
	Page getCalliForbidden();

	@Iri("http://callimachusproject.org/rdf/2009/framework#forbidden")
	void setCalliForbidden(Page forbidden);

	/**
	 * The RDFa template used when an agent is aunothorized from the requested
	 * resource.
	 */
	@Iri("http://callimachusproject.org/rdf/2009/framework#unauthorized")
	Page getCalliUnauthorized();

	@Iri("http://callimachusproject.org/rdf/2009/framework#unauthorized")
	void setCalliUnauthorized(Page unauthorized);
}
