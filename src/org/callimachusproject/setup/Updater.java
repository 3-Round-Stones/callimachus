package org.callimachusproject.setup;

import java.io.IOException;

import org.callimachusproject.server.CallimachusRepository;
import org.openrdf.OpenRDFException;

public interface Updater {

	boolean update(String webapp, CallimachusRepository repository) throws IOException, OpenRDFException;

}
