package org.callimachusproject.api;

import java.io.File;

import org.callimachusproject.Server;

public class TemporaryServer {
	private Server server;
	private static TemporaryServer temporaryServer = new TemporaryServer();
	private String origin = "http://localhost:8080";
	
	public TemporaryServer() {
		server = new Server();
		server.init(new String[]{"-p", "8080", "-o", origin, "-r", "repositories/callimachus", "-trust"});
	}
	
	public void start() throws Exception {
		server.start();
	}
	
	public void stop() throws Exception {
		server.stop();
	}
	
	public static TemporaryServer getInstance() {
		return temporaryServer;
	}
	
	public String getOrigin() {
		return origin;
	}
	
}
