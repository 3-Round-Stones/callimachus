package org.callimachusproject.setup;

import java.io.IOException;

public abstract class UpdateProvider {

	public String getDefaultWebappLocation(String origin) throws IOException {
		return null;
	}

	public Updater prepareCallimachusWebapp(String origin) throws IOException {
		return null;
	}

	public Updater updateFrom(String origin, String version) throws IOException {
		return null;
	}

	public Updater updateCallimachusWebapp(String origin) throws IOException {
		return null;
	}

	public Updater updateOrigin(String virtual) throws IOException {
		return null;
	}

	public Updater finalizeCallimachusWebapp(String origin) throws IOException {
		return null;
	}
}
