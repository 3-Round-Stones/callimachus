package org.callimachusproject.server.behaviours;

import java.io.IOException;
import java.net.URL;

import org.callimachusproject.server.annotations.method;
import org.callimachusproject.server.annotations.query;
import org.callimachusproject.server.concepts.Alias;

public abstract class AliasSupport implements Alias {
	@query({})
	@method("GET")
	public URL getAlias() throws IOException {
		return getRedirectsTo().toUri().toURL();
	}

}
