package org.callimachusproject.concepts;

import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.RDFObject;

@Iri("http://callimachusproject.org/rdf/2009/framework#DigestManager")
public interface DigestManager extends AuthenticationManager {

	@Iri("http://callimachusproject.org/rdf/2009/framework#authName")
	String getAuthName();

	@Iri("http://callimachusproject.org/rdf/2009/framework#authName")
	void setAuthName(String authName);

	@Iri("http://callimachusproject.org/rdf/2009/framework#authNamespace")
	RDFObject getAuthNamespace();

	@Iri("http://callimachusproject.org/rdf/2009/framework#authNamespace")
	void setAuthNamespace(RDFObject authNamespace);

}
