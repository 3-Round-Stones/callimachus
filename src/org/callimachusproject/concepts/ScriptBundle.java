/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.concepts;

import java.io.IOException;
import java.util.Set;

import org.callimachusproject.annotations.requires;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Method;
import org.openrdf.annotations.Path;
import org.openrdf.annotations.Type;
import org.openrdf.http.object.exceptions.GatewayTimeout;

@Iri("http://callimachusproject.org/rdf/2009/framework#ScriptBundle")
public interface ScriptBundle {

	/** Level of minification that should be applied to this bundle. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#minified")
	Set<Number> getCalliMinified();

	/** Level of minification that should be applied to this bundle. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#minified")
	void setCalliMinified(Set<Number> minified);

	@Method("GET")
	@Path("?source")
	@Type("text/javascript")
	@requires("http://callimachusproject.org/rdf/2009/framework#reader")
	String calliGetBundleSource() throws GatewayTimeout, IOException, OpenRDFException;

	@Method("GET")
	@Path("?minified")
	@Type("text/javascript")
	@requires("http://callimachusproject.org/rdf/2009/framework#reader")
	String calliGetMinifiedBundle() throws Exception;
}
