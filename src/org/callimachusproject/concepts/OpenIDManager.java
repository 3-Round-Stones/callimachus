package org.callimachusproject.concepts;

import org.openrdf.annotations.Iri;

@Iri("http://callimachusproject.org/rdf/2009/framework#OpenIDManager")
public interface OpenIDManager extends AuthenticationManager {
	@Iri("http://callimachusproject.org/rdf/2009/framework#openIdEndpointUrl")
	String getOpenIdEndpointUrl();

	@Iri("http://callimachusproject.org/rdf/2009/framework#openIdEndpointUrl")
	void getOpenIdEndpointUrl(String url);

	@Iri("http://callimachusproject.org/rdf/2009/framework#openIdRealm")
	String getOpenIdRealm();

	@Iri("http://callimachusproject.org/rdf/2009/framework#openIdRealm")
	void getOpenIdRealm(String openIdRealm);
}
