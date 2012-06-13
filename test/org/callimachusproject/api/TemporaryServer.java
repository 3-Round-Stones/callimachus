package org.callimachusproject.api;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.callimachusproject.Server;
import org.callimachusproject.Setup;

public class TemporaryServer {
	private static final String DEFAULT_USERNAME = "test";
	private static final char[] DEFAULT_PASSWORD = "test".toCharArray();
	private static int DEFAULT_PORT = 49152;

	public static synchronized TemporaryServer start() throws Exception {
		String origin = "http://localhost:" + DEFAULT_PORT;
		File dataDir = createCallimachus(origin);
		return new TemporaryServer(origin, DEFAULT_PORT, dataDir, true);
	}

	private static File createCallimachus(String origin) throws Exception {
		String name = TemporaryServer.class.getSimpleName();
		File dir = createDirectory(name);
		File car = findCallimachusWebappCar();
		if (!dir.isDirectory() || car.lastModified() > dir.lastModified()) {
			if (dir.isDirectory()) {
				FileUtil.deleteDir(dir);
			}
			dir.delete();
			String config = new Scanner(TemporaryServer.class
					.getResourceAsStream("/callimachus-repository.ttl")).useDelimiter("\\A").next();
			Setup setup = new Setup();
			setup.connect(dir, config);
			setup.createOrigin(origin);
			setup.importCar(car.toURI().toURL(), origin + "/callimachus/", origin);
			setup.setServeAllResourcesAs(origin);
			setup.createAdmin(DEFAULT_USERNAME, null, DEFAULT_USERNAME, DEFAULT_PASSWORD, origin);
			setup.disconnect();
		}
		File temp = FileUtil.createTempDir(name);
		copyDir(dir, temp);
		return temp;
	}

	private static void copyDir(File src, File dest) throws IOException {
		if (src.isDirectory()) {
			dest.mkdir();
			for (String file : src.list()) {
				copyDir(new File(src, file), new File(dest, file));
			}
		} else {
			FileUtil.copyFile(src, dest);
		}
	}

	private static File findCallimachusWebappCar() throws IOException {
		File dist = new File("dist");
		if (dist.list() != null) {
			for (String file : dist.list()) {
				if (file.startsWith("callimachus-webapp")
						&& file.endsWith(".car"))
					return new File(dist, file);
			}
		}
		throw new IOException("Could not find callimachus-webapp.car in "
				+ dist.getAbsolutePath());
	}

	private static File createDirectory(String name) throws IOException {
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr != null) {
			File tmpDir = new File(tmpDirStr);
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
		}
		File tmp = File.createTempFile(name, "");
		tmp.delete();
		return new File(tmp.getParentFile(), name);
	}

	private final Server server;
	private final String origin;
	private final File dir;
	private final boolean delete;

	public TemporaryServer(String origin, int port, File dir, boolean delete) throws Exception {
		this.origin = origin;
		this.dir = dir;
		this.delete = delete;
		server = new Server();
		File dataDir = new File(new File(dir, "repositories"), "callimachus");
		String uri = dataDir.toURI().toASCIIString();
		String p = String.valueOf(port);
		Thread.yield();
		server.init(new String[] { "-p", p, "-o", origin, "-r", uri, "-trust" });
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
		server.destroy();
		if (delete) {
			FileUtil.deltree(dir);
		}
		Thread.yield();
	}

	public String getOrigin() {
		return origin;
	}

	public String getUsername() {
		return DEFAULT_USERNAME;
	}

	public char[] getPassword() {
		return DEFAULT_PASSWORD;
	}

}
