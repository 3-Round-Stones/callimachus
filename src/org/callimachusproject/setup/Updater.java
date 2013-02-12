package org.callimachusproject.setup;

import java.io.IOException;

import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;

public interface Updater {

	boolean update(String webapp, CalliRepository repository) throws IOException, OpenRDFException;

}
