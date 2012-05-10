package org.callimachusproject.logging.trace;

public interface TracerFactory {

	<T> T trace(MethodCall returnedFrom, T target, Class<T> cls, TracerService service);

}
