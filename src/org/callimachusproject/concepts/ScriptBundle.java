package org.callimachusproject.concepts;

import java.io.IOException;
import java.util.Set;

import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.exceptions.GatewayTimeout;
import org.openrdf.OpenRDFException;
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
	@requires("http://callimachusproject.org/rdf/2009/framework#reader")
	String calliGetBundleSource() throws GatewayTimeout, IOException, OpenRDFException;

	@method("GET")
	@query("minified")
	@type("text/javascript")
	@requires("http://callimachusproject.org/rdf/2009/framework#reader")
	String calliGetMinifiedBundle() throws GatewayTimeout, IOException, OpenRDFException;
}
