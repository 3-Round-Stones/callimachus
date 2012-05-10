package org.callimachusproject.logging.trace;

public interface TracerProvider {

	TracerFactory getTracerFactory(Class<?> cls);

}
