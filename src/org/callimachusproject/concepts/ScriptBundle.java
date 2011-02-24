package org.callimachusproject.concepts;

import java.io.IOException;

import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.repository.object.annotations.iri;

@iri("http://callimachusproject.org/rdf/2009/framework#ScriptBundle")
public interface ScriptBundle {

	/** Level of minification that should be applied to this bundle. */
	@iri("http://callimachusproject.org/rdf/2009/framework#minified")
	Number getCalliMinified();

	/** Level of minification that should be applied to this bundle. */
	@iri("http://callimachusproject.org/rdf/2009/framework#minified")
	void setCalliMinified(Number scripts);

	@method("GET")
	@query("source")
	@type("text/javascript")
	String calliGetBundleSource() throws GatewayTimeout, IOException;
}
