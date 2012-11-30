package org.callimachusproject.setup;

import java.io.InputStream;

public class RobotsProvider extends FileProvider {
	private static final String TEMPLATE = "META-INF/templates/robots.txt";
	private static final String TEXT_TYPE = "types/TextFile";
	private static final String ROBOTS_TXT = "/robots.txt";

	@Override
	protected InputStream getFileResourceAsStream() {
		ClassLoader cl = getClass().getClassLoader();
		return cl.getResourceAsStream(TEMPLATE);
	}

	@Override
	protected String getFileUrl(String virtual) {
		return virtual + ROBOTS_TXT;
	}

	@Override
	protected String getFileType(String webapps) {
		return webapps + TEXT_TYPE;
	}

}
