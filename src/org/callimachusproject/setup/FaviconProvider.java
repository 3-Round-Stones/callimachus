package org.callimachusproject.setup;

import java.io.IOException;
import java.io.InputStream;

public class FaviconProvider extends UpdateProvider {
	private static final String FAVICON = "META-INF/templates/callimachus-icon.ico";
	private static final String ICO_TYPE = "types/IconGraphic";

	@Override
	public Updater updateOrigin(String virtual) throws IOException {
		return new FileUpdater(virtual) {

			@Override
			protected String getFileUrl(String origin) {
				return origin + "/favicon.ico";
			}

			@Override
			protected String[] getFileType(String webapps) {
				return new String[] { webapps + ICO_TYPE,
						"http://xmlns.com/foaf/0.1/Image" };
			}

			@Override
			protected InputStream getFileResourceAsStream() {
				ClassLoader cl = getClass().getClassLoader();
				return cl.getResourceAsStream(FAVICON);
			}
		};
	}

}
