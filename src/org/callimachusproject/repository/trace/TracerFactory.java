package org.callimachusproject.repository.trace;

public interface TracerFactory {

	<T> T trace(MethodCall returnedFrom, T target, Class<T> cls, TracerService service);

}
