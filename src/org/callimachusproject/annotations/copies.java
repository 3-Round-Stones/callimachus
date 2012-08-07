package org.callimachusproject.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openrdf.annotations.Iri;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER })
public @interface copies {
	@Iri("http://callimachusproject.org/rdf/2009/framework#copies")
	String[] value();
}
