package org.openrdf.http.object.concepts;

import org.openrdf.annotations.Iri;

@Iri("http://www.openrdf.org/rdf/2009/auditing#Alias")
public interface Alias {

	@Iri("http://www.openrdf.org/rdf/2009/httpobject#redirectsTo")
	HTTPFileObject getRedirectsTo();

	void setRedirectsTo(HTTPFileObject redirectsTo);

}
