package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;

public class RobotsProvider extends UpdateProvider {
	private static final String TEMPLATE = "META-INF/templates/robots.txt";
	private static final String TEXT_TYPE = "types/TextFile";
	private static final String ROBOTS_TXT = "/robots.txt";

	@Override
	public Updater updateOrigin(String virtual) throws IOException {
		return new FileUpdater(virtual) {

			@Override
			protected String getFileUrl(String origin) {
				return origin + ROBOTS_TXT;
			}

			@Override
			protected String[] getFileType(String webapps) {
				return new String[] { webapps + TEXT_TYPE,
						"http://xmlns.com/foaf/0.1/Document" };
			}

			@Override
			protected InputStream getFileResourceAsStream() {
				ClassLoader cl = getClass().getClassLoader();
				return cl.getResourceAsStream(TEMPLATE);
			}
		};
	}

}
