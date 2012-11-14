package org.callimachusproject.concepts;

import org.openrdf.annotations.Iri;

@Iri("http://callimachusproject.org/rdf/2009/framework#DigestManager")
public interface DigestManager extends AuthenticationManager {

	@Iri("http://callimachusproject.org/rdf/2009/framework#authName")
	String getAuthName();

	@Iri("http://callimachusproject.org/rdf/2009/framework#authName")
	void setAuthName(String authName);

}
