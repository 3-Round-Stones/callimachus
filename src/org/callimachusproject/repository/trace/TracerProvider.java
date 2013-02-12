package org.callimachusproject.repository.trace;

public interface TracerProvider {

	TracerFactory getTracerFactory(Class<?> cls);

}
