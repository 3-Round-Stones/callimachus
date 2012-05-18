package org.callimachusproject.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openrdf.annotations.Iri;
/**
 * @see script
 * @author James Leigh
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.PARAMETER, ElementType.TYPE, ElementType.METHOD })
public @interface imports {
	@Iri("http://callimachusproject.org/rdf/2009/framework#imports")
	Class<?>[] value();
}
