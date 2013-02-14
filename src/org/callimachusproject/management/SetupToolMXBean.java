package org.callimachusproject.management;

import java.io.IOException;
import java.util.Map;

import org.callimachusproject.setup.SetupOrigin;
import org.openrdf.OpenRDFException;

public interface SetupToolMXBean {

	boolean isSetupInProgress();

	void checkForErrors() throws Exception;

	String[] getAvailableRepositoryTypes() throws IOException;

	Map<String, String> getRepositoryProperties() throws IOException;

	void setRepositoryProperties(Map<String,String> properties) throws IOException, OpenRDFException;

	String getWebappOrigins() throws IOException;

	void setWebappOrigins(String origins) throws Exception;

	SetupOrigin[] getOrigins() throws IOException, OpenRDFException;

	void addResolvableOrigin(String origin, String webappOrigin) throws Exception;

	void addRootRealm(String realm, String webappOrigin) throws Exception;

	void importCar(String url, String folder) throws Exception;

	String[] getDigestEmailAddresses(String webappOrigin)
			throws OpenRDFException, IOException;

	void inviteAdminUser(String email, String username, String label,
			String comment, String subject, String body, String webappOrigin)
			throws Exception;

	boolean changeDigestUserPassword(String email, String password,
			String webappOrigin) throws OpenRDFException, IOException;
}
