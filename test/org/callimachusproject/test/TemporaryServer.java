package org.callimachusproject.test;

import org.callimachusproject.server.CallimachusRepository;

public interface TemporaryServer {

	void start() throws InterruptedException, Exception;

	void pause() throws Exception;

	void resume() throws Exception;

	void stop() throws Exception;

	void destroy() throws Exception;

	String getOrigin();

	String getUsername();

	char[] getPassword();

	CallimachusRepository getRepository();

}