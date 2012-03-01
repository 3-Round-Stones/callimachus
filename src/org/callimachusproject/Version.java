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
	private static Version instance = new Version(loadVersion(Version.class.getClassLoader(), VERSION_PATH));

	public static void main(String[] args) {
		System.out.println(Version.getInstance().getVersion());
	}

	public static Version getInstance() {
		return instance;
	}

	private static String loadVersion(ClassLoader cl, String properties) {
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

	private final String product = "Callimachus";
	private final int major;
	private final int release;
	private final int maintenance;
	private final String qualifier;
	private final int development;

	Version(String version) {
		major = parseInt(version, "(\\d+)\\b.*");
		release = parseInt(version, "\\d+\\.(\\d+)\\b.*");
		maintenance = parseInt(version, "\\d+\\.\\d+\\.(\\d+)\\b.*");
		qualifier = parseString(version, "[\\d\\.]+-([^\\d-]+)\\d*");
		development = parseInt(version, "[\\d\\.]+-[^\\d-]*(\\d+)");
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
	public CharSequence getVersion() {
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
		return sb;
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
	public String getQualifierVersion() {
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

}
