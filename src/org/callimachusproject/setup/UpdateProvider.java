package org.callimachusproject.setup;

import java.io.IOException;

public interface UpdateProvider {

	String getDefaultCallimachusWebappLocation(String origin) throws IOException;

	Updater updateOrigin(String virtual) throws IOException;

	Updater updateRealm(String realm) throws IOException;

	Updater updateFrom(String origin, String version) throws IOException;

	Updater updateCallimachusWebapp(String origin) throws IOException;
}
