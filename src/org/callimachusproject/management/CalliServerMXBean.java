package org.callimachusproject.management;

import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;

import org.callimachusproject.setup.SetupRealm;
import org.openrdf.OpenRDFException;

public interface CalliServerMXBean {

	boolean isRunning();

	void init() throws IOException, OpenRDFException;

	void start() throws IOException, OpenRDFException;

	void stop() throws IOException, OpenRDFException;

	void destroy() throws OpenRDFException, IOException;

	String getServerName() throws IOException;

	void setServerName(String name) throws IOException;

	String getPorts() throws IOException;

	void setPorts(String ports) throws IOException;

	String getSSLPorts() throws IOException;

	void setSSLPorts(String ports) throws IOException;

	boolean isStartingInProgress();

	boolean isStoppingInProgress();

	boolean isWebServiceRunning();

	boolean isSetupInProgress();

	void checkForErrors() throws Exception;

	void startWebService() throws Exception;

	void restartWebService() throws Exception;

	void stopWebService() throws Exception;

	Map<String, String> getMailProperties() throws IOException;

	void setMailProperties(Map<String, String> lines) throws IOException,
			MessagingException;

	Map<String, String> getLoggingProperties() throws IOException;

	void setLoggingProperties(Map<String, String> lines) throws IOException,
			MessagingException;

	String[] getRepositoryIDs() throws OpenRDFException;

	String[] getAvailableRepositoryTypes() throws IOException;

	Map<String, String> getRepositoryProperties() throws IOException, OpenRDFException;

	void setRepositoryProperties(Map<String,String> properties) throws IOException, OpenRDFException;

	String[] getWebappOrigins() throws IOException;

	SetupRealm[] getRealms() throws IOException, OpenRDFException;

	Map<String, String> getAuthenticationManagers() throws OpenRDFException, IOException;

	void setupWebappOrigin(String webappOrigin, String repositoryID) throws Exception;

	void ignoreWebappOrigin(String webappOrigin) throws Exception;

	void setupRealm(String realm, String webappOrigin) throws Exception;

	void createResource(String rdf, String systemId, String type) throws Exception;

	String[] getDigestEmailAddresses(String webappOrigin)
			throws OpenRDFException, IOException;

	void inviteAdminUser(String email, String username, String label,
			String comment, String subject, String body, String webappOrigin)
			throws Exception;

	boolean changeDigestUserPassword(String email, String password,
			String webappOrigin) throws OpenRDFException, IOException;

}