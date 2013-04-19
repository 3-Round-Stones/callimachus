package org.callimachusproject.util;

import java.io.File;
import java.io.IOException;

import org.callimachusproject.Version;

public class SystemProperties {
	private static final String SERVER_DEFAULT_CONF = "etc/callimachus-defaults.conf";
	private static final String REPOSITORY_CONFIG = "etc/callimachus-repository.ttl";
	private static final String VERSION_CODE = Version.getInstance().getVersionCode();
	private static final String WEBAPP_CAR = "lib/callimachus-webapp-" + VERSION_CODE + ".car";

	public static char[] getKeyStorePassword() throws IOException {
		String password = System.getProperty("javax.net.ssl.keyStorePassword");
		if (password == null)
			return "changeit".toCharArray();
		return password.toCharArray();
	}

	public static File getKeyStoreFile() throws IOException {
		String keyStore = System.getProperty("javax.net.ssl.keyStore");
		if (keyStore == null)
			return new File(".keystore");
		return new File(keyStore);
	}

	public static File getMailPropertiesFile() {
		String mail = System.getProperty("java.mail.properties");
		if (mail == null)
			return new File("etc/mail.properties");
		return new File(mail);
	}

	public static File getLoggingPropertiesFile() {
		String file = System.getProperty("java.util.logging.config.file");
		if (file == null)
			return new File("etc/logging.properties");
		return new File(file);
	}

	public static File getRepositoryConfigFile() {
		String rconfig = System
				.getProperty("org.callimachusproject.config.repository");
		if (rconfig != null)
			return new File(rconfig);
		return new File(REPOSITORY_CONFIG);
	}

	public static File getWebappCarFile() {
		String car = System
				.getProperty("org.callimachusproject.config.webapp");
		if (car != null)
			return new File(car);
		return new File(WEBAPP_CAR);
	}

	public static File getConfigDefaultsFile() {
		String defaultFile = System
				.getProperty("org.callimachusproject.config.defaults");
		if (defaultFile != null)
			return new File(defaultFile);
		return new File(SERVER_DEFAULT_CONF);
	}

	public static File getBackupDirectory() {
		String defaultFile = System
				.getProperty("org.callimachusproject.config.backups");
		if (defaultFile != null)
			return new File(defaultFile);
		return null;
	}
}
