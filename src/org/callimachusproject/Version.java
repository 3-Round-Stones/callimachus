package org.callimachusproject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.callimachusproject.server.WebServer;
import org.slf4j.LoggerFactory;

public class Version {
	private static final String VERSION_PATH = "META-INF/callimachusproject.properties";
	private static Version instance = new Version(loadVersion(Version.class.getClassLoader(), VERSION_PATH));

	public static void main(String[] args) {
		System.out.println(Version.getInstance().getVersion());
	}

	public static Version getInstance() {
		return instance;
	}

	private static String loadVersion(ClassLoader cl, String properties) {
		String implVer = Version.class.getPackage().getImplementationVersion();
		if (implVer != null)
			return implVer;
		try {
			InputStream in = cl.getResourceAsStream(properties);
			if (in != null) {
				try {
					Properties result = new Properties();
					result.load(in);
					String version = result.getProperty("Version");
					if (version != null)
						return version.trim();
				} finally {
					in.close();
				}
			}
		} catch (IOException e) {
			LoggerFactory.getLogger(WebServer.class).warn(
					"Unable to read version info", e);
		}
		return "devel";
	}

	private final String product = "Callimachus";
	private final int major;
	private final int release;
	private final int maintenance;
	private final String qualifier;
	private final int development;
	private final String build;

	Version(String version) {
		major = parseInt(version, "([0-9]+)\\b.*");
		release = parseInt(version, "[0-9]+\\.([0-9]+)\\b.*");
		maintenance = parseInt(version, "[0-9]+\\.[0-9]+\\.([0-9]+)\\b.*");
		qualifier = parseString(version, "[0-9\\.]+-([A-Za-z\\-]+?)(\\-?[0-9]+)?(\\+[0-9A-Za-z\\-\\.]+)?");
		development = parseInt(version, "[0-9\\.]+-[A-Za-z\\-]*([0-9]+)(\\+[0-9A-Za-z\\-\\.]+)?");
		build = parseString(version, "[0-9A-Za-z\\.\\-]*\\+([0-9A-Za-z\\-\\.]+)");
	}

	private int parseInt(String version, String pattern) {
		String str = parseString(version, pattern);
		if (str == null)
			return 0;
		return Integer.parseInt(str);
	}

	private String parseString(String version, String pattern) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(version);
		if (m.matches()) {
			return m.group(1);
		} else {
			return null;
		}
	}

	/**
	 * Get the basic version string for the current Callimachus.
	 * 
	 * @return String denoting our current version
	 */
	public String getVersion() {
		StringBuilder sb = new StringBuilder();
		sb.append(getProduct());
		sb.append("/").append(getVersionCode());
		return sb.toString();
	}

	/**
	 * Get the basic version string for the current Callimachus.
	 * 
	 * @return String denoting our current version
	 */
	public String getVersionCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(getMajorVersionNum());
		sb.append(".").append(getReleaseVersionNum());
		if (getMaintenanceVersionNum() > 0) {
			sb.append(".").append(getMaintenanceVersionNum());
		}
		if (getQualifierIdentifier() != null) {
			sb.append("-").append(getQualifierIdentifier());
			if (getDevelopmentVersionNum() > 0) {
				sb.append(getDevelopmentVersionNum());
			}
		} else if (getDevelopmentVersionNum() > 0) {
			sb.append("-").append(getDevelopmentVersionNum());
		}
		if (getBuildIdentifier() != null) {
			sb.append("+").append(getBuildIdentifier());
		}
		return sb.toString();
	}

	/**
	 * Name of product: Callimachus.
	 */
	public String getProduct() {
		return product;
	}

	/**
	 * Major version number. Version number. This changes only when there is a
	 * significant, externally apparent enhancement from the previous release.
	 * 'n' represents the n'th version.
	 * 
	 * Clients should carefully consider the implications of new versions as
	 * external interfaces and behaviour may have changed.
	 */
	public int getMajorVersionNum() {
		return major;

	}

	/**
	 * Release Number. Release number. This changes when: - a new set of
	 * functionality is to be added. - API or behaviour change. - its designated
	 * as a reference release.
	 */
	public int getReleaseVersionNum() {
		return release;
	}

	/**
	 * Maintenance Drop Number. Optional identifier used to designate
	 * maintenance drop applied to a specific release and contains fixes for
	 * defects reported. It maintains compatibility with the release and
	 * contains no API changes. When missing, it designates the final and
	 * complete development drop for a release.
	 */
	public int getMaintenanceVersionNum() {
		return maintenance;
	}

	/**
	 * The qualifier exists to capture milestone builds: alpha and beta
	 * releases, and the qualifier is separated from the major, minor, and
	 * incremental versions by a hyphen.
	 */
	public String getQualifierIdentifier() {
		return qualifier;
	}

	/**
	 * Development Drop Number. Optional identifier designates development drop
	 * of a specific release.
	 * 
	 * Development drops are works in progress towards a compeleted, final
	 * release. A specific development drop may not completely implement all
	 * aspects of a new feature, which may take several development drops to
	 * complete.
	 */
	public int getDevelopmentVersionNum() {
		return development;
	}

	/**
	 * Optional build identifier designates a specific binary build of a
	 * version.
	 * 
	 * Build has no barring on the software version precedence, but is used to
	 * identify the particular process that converted this software into binary.
	 */
	public String getBuildIdentifier() {
		return build;
	}

}
