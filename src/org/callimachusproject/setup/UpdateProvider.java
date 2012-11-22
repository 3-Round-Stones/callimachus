package org.callimachusproject.setup;

import java.io.IOException;

public interface UpdateProvider {

	String getDefaultCallimachusWebappLocation(String origin) throws IOException;

	Updater initialize(String origin) throws IOException;

	Updater updateFrom(String origin, String version) throws IOException;

	Updater update(String origin) throws IOException;
}
