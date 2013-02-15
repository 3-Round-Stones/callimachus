package org.callimachusproject.management;

import java.io.IOException;

import org.openrdf.OpenRDFException;

public interface CalliServerMXBean {

	boolean isRunning();

	void init() throws IOException, OpenRDFException;

	void start() throws IOException, OpenRDFException;

	void stop() throws IOException;

	void destroy();

	String getServerName() throws IOException;

	void setServerName(String name) throws IOException;

	String getPorts() throws IOException;

	void setPorts(String ports) throws IOException;

	String getSSLPorts() throws IOException;

	void setSSLPorts(String ports) throws IOException;

	boolean isStartingInProgress();

	boolean isStoppingInProgress();

	boolean isWebServiceRunning();

	void startWebService() throws Exception;

	void stopWebService() throws Exception;

	String[] sparqlQuery(String query) throws OpenRDFException, IOException;

	void sparqlUpdate(String update) throws OpenRDFException, IOException;

}