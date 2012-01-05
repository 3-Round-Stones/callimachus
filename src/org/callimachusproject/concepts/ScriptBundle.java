package org.callimachusproject.concepts;

import java.io.IOException;
import java.util.Set;

import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.annotations.Iri;

@Iri("http://callimachusproject.org/rdf/2009/framework#ScriptBundle")
public interface ScriptBundle {

	/** Level of minification that should be applied to this bundle. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#minified")
	Set<Number> getCalliMinified();

	/** Level of minification that should be applied to this bundle. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#minified")
	void setCalliMinified(Set<Number> minified);

	@method("GET")
	@query("source")
	@type("text/javascript")
	String calliGetBundleSource() throws GatewayTimeout, IOException;
}
