package org.callimachusproject.concepts;

import java.util.Set;

import org.openrdf.annotations.Iri;

/**
 * Class of resources that contain nested components, where the component URIs
 * have their parent URI as a prefix
 */
@Iri("http://callimachusproject.org/rdf/2009/framework#Composite")
public interface Composite {
	/**
	 * A nested resource with a URI that starts with this resource URI with one
	 * additional path, fragement, or component segment
	 */
	@Iri("http://callimachusproject.org/rdf/2009/framework#hasComponent")
	Set<Object> getCalliHasComponent();

	/**
	 * A nested resource with a URI that starts with this resource URI with one
	 * additional path, fragement, or component segment
	 */
	@Iri("http://callimachusproject.org/rdf/2009/framework#hasComponent")
	void setCalliHasComponent(Set<?> calliHasComponent);

}