package org.callimachusproject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.callimachusproject.server.HTTPObjectServer;
import org.slf4j.LoggerFactory;

public class Version {
	private static final String VERSION_PATH = "META-INF/callimachusproject.properties";
	private static final String VERSION;
	private static final int MAJOR;
	private static final int RELEASE;
	private static final int MAINTENANCE;
	private static final String QUALIFIER;
	private static final int DEVELOPMENT;

	static {
		VERSION = loadVersion(Version.class.getClassLoader(), VERSION_PATH);
		Pattern p = Pattern.compile("(\\d+)\\b.*");
		Matcher m = p.matcher(VERSION);
		if (m.matches()) {
			MAJOR = Integer.parseInt(m.group(1));
		} else {
			MAJOR = 0;
		}
		p = Pattern.compile("\\d+\\.(\\d+)\\b.*");
		m = p.matcher(VERSION);
		if (m.matches()) {
			RELEASE = Integer.parseInt(m.group(1));
		} else {
			RELEASE = 0;
		}
		p = Pattern.compile("\\d+\\.\\d+\\.(\\d+)\\b.*");
		m = p.matcher(VERSION);
		if (m.matches()) {
			MAINTENANCE = Integer.parseInt(m.group(1));
		} else {
			MAINTENANCE = 0;
		}
		p = Pattern.compile("[\\d\\.]*-([^\\d-]*).*");
		m = p.matcher(VERSION);
		if (m.matches()) {
			QUALIFIER = m.group(1);
		} else {
			QUALIFIER = null;
		}
		p = Pattern.compile(".*\\b(\\d+)");
		m = p.matcher(VERSION);
		if (m.matches()) {
			DEVELOPMENT = Integer.parseInt(m.group(1));
		} else {
			DEVELOPMENT = 0;
		}
	}

	public static String loadVersion(ClassLoader cl, String properties) {
		try {
			InputStream in = cl.getResourceAsStream(properties);
			if (in != null) {
				try {
					Properties result = new Properties();
					result.load(in);
					String version = result.getProperty("version");
					if (version != null)
						return version.trim();
				} finally {
					in.close();
				}
			}
		} catch (IOException e) {
			LoggerFactory.getLogger(HTTPObjectServer.class).warn(
					"Unable to read version info", e);
		}
		return "devel";
	}

	public static void main(String[] args) {
		System.out.println(getVersion());
	}

	/**
	 * Get the basic version string for the current Callimachus.
	 * 
	 * @return String denoting our current version
	 */
	public static String getVersion() {
		StringBuilder sb = new StringBuilder();
		sb.append(getProduct());
		sb.append(" ").append(getMajorVersionNum());
		sb.append(".").append(getReleaseVersionNum());
		if (getMaintenanceVersionNum() > 0) {
			sb.append(".").append(getMaintenanceVersionNum());
		}
		if (getQualifierVersion() != null) {
			sb.append("-").append(getQualifierVersion());
		}
		if (getDevelopmentVersionNum() > 0) {
			sb.append("-").append(getDevelopmentVersionNum());
		}
		return sb.toString();
	}

	/**
	 * Name of product: Callimachus.
	 */
	public static String getProduct() {
		return "Callimachus";
	}

	/**
	 * Major version number. Version number. This changes only when there is a
	 * significant, externally apparent enhancement from the previous release.
	 * 'n' represents the n'th version.
	 * 
	 * Clients should carefully consider the implications of new versions as
	 * external interfaces and behaviour may have changed.
	 */
	public static int getMajorVersionNum() {
		return MAJOR;

	}

	/**
	 * Release Number. Release number. This changes when: - a new set of
	 * functionality is to be added. - API or behaviour change. - its designated
	 * as a reference release.
	 */
	public static int getReleaseVersionNum() {
		return RELEASE;
	}

	/**
	 * Maintenance Drop Number. Optional identifier used to designate
	 * maintenance drop applied to a specific release and contains fixes for
	 * defects reported. It maintains compatibility with the release and
	 * contains no API changes. When missing, it designates the final and
	 * complete development drop for a release.
	 */
	public static int getMaintenanceVersionNum() {
		return MAINTENANCE;
	}

	/**
	 * The qualifier exists to capture milestone builds: alpha and beta
	 * releases, and the qualifier is separated from the major, minor, and
	 * incremental versions by a hyphen.
	 */
	public static String getQualifierVersion() {
		return QUALIFIER;
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
	public static int getDevelopmentVersionNum() {
		return DEVELOPMENT;
	}

}
