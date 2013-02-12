package org.callimachusproject.test;

import org.callimachusproject.repository.CalliRepository;

public interface TemporaryServer {

	void start() throws InterruptedException, Exception;

	void pause() throws Exception;

	void resume() throws Exception;

	void stop() throws Exception;

	void destroy() throws Exception;

	String getOrigin();

	String getUsername();

	char[] getPassword();

	CalliRepository getRepository();

}