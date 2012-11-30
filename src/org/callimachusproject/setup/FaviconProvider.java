package org.callimachusproject.setup;

import java.io.InputStream;

public class FaviconProvider extends FileProvider {
	private static final String FAVICON = "META-INF/templates/callimachus-icon.ico";
	private static final String ICO_TYPE = "types/IconGraphic";

	@Override
	protected InputStream getFileResourceAsStream() {
		ClassLoader cl = getClass().getClassLoader();
		return cl.getResourceAsStream(FAVICON);
	}

	@Override
	protected String getFileUrl(String virtual) {
		return virtual + "/favicon.ico";
	}

	@Override
	protected String getFileType(String webapps) {
		return webapps + ICO_TYPE;
	}

}
