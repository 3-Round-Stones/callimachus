/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.server;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.management.ManagementPermission;
import java.lang.reflect.ReflectPermission;
import java.net.MalformedURLException;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.logging.LoggingPermission;

import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;
import javax.management.MBeanTrustPermission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restricts files system access.
 *
 * @author James Leigh
 */
public class HTTPObjectPolicy extends Policy {
	private static Logger logger = LoggerFactory.getLogger(HTTPObjectPolicy.class);
	private static CodeSource source = HTTPObjectPolicy.class
			.getProtectionDomain().getCodeSource();
	/** loaded from a writable location */
	private final PermissionCollection plugins;
	private final PermissionCollection jars;
	private final List<String> writableLocations;

	public static boolean apply(String[] readable, File... writable) {
		try {
			Policy.setPolicy(new HTTPObjectPolicy(readable, writable));
			System.setSecurityManager(new SecurityManager());
			logger.info("Restricted file system access in effect");
			return true;
		} catch (SecurityException e) {
			// a policy must already be applied
			logger.debug(e.toString(), e);
			return false;
		}
	}

	@Override
	public PermissionCollection getPermissions(CodeSource codesource) {
		if (source == codesource || source != null && source.equals(codesource))
			return copy(jars);
		if (codesource == null || codesource.getLocation() == null)
			return copy(plugins);
		String location = codesource.getLocation().toExternalForm();
		if (!location.startsWith("file:"))
			return copy(plugins);
		for (String url : writableLocations) {
			if (location.startsWith(url))
				return copy(plugins);
		}
		return copy(jars);
	}

	private HTTPObjectPolicy(String[] readable, File... directories) {
		plugins = new Permissions();
		plugins.add(new PropertyPermission("*", "read"));
		plugins.add(new PropertyPermission("apple.awt.graphics.*", "write"));
		plugins.add(new RuntimePermission("getenv.*"));
		plugins.add(new SocketPermission("*", "connect,resolve"));
		plugins.add(new SocketPermission("*", "accept,listen"));
		plugins.add(new NetPermission("getProxySelector"));
		plugins.add(new ReflectPermission("suppressAccessChecks"));
		plugins.add(new MBeanServerPermission("createMBeanServer"));
		plugins.add(new SecurityPermission("putProviderProperty.*"));
		plugins.add(new RuntimePermission("accessDeclaredMembers"));
		plugins.add(new RuntimePermission("getClassLoader"));
		plugins.add(new RuntimePermission("createClassLoader"));
		plugins.add(new RuntimePermission("accessDeclaredMembers"));
		plugins.add(new RuntimePermission("getProtectionDomain"));
		plugins.add(new RuntimePermission("setContextClassLoader"));
		plugins.add(new RuntimePermission("accessClassInPackage.*"));
		File home = new File(System.getProperty("user.home"));
		Set<String> visited = new HashSet<String>();
		addReadableDirectory(new File(home, ".mime-types.properties"), visited);
		addReadableDirectory(new File(home, ".mime.types"), visited);
		addReadableDirectory(new File(home, ".magic.mime"), visited);
		for (Object value : getSystemPropertyValues()) {
			if (value instanceof String) {
				File file = new File((String) value);
				if (file.exists()) {
					addReadableDirectory(file, visited);
				}
			}
		}
		plugins.add(new FilePermission(home.getAbsolutePath() + "/.local/-", "read"));
		plugins.add(new FilePermission("/etc/magic.mime", "read"));
		plugins.add(new FilePermission("/usr/-", "read"));
		plugins.add(new FilePermission("/opt/-", "read"));
		plugins.add(new FilePermission("/System/-", "read"));
		plugins.add(new FilePermission("/Applications/-", "read"));
		plugins.add(new FilePermission("C:\\WINDOWS\\-", "read"));
		plugins.add(new FilePermission("C:\\Program Files\\-", "read"));
		addClassPath(System.getProperty("java.ext.dirs"));
		addClassPath(System.getProperty("java.endorsed.dirs"));
		addClassPath(System.getProperty("java.class.path"));
		addClassPath(System.getProperty("sun.boot.class.path"));
		addClassPath(System.getProperty("jdk.home"));
		addClassPath(System.getProperty("java.home"));
		addClassPath(new File("").getAbsolutePath());
		addClassPath(readable);
		writableLocations = new ArrayList<String>(directories.length + 1);
		addWritableDirectories(directories);
		jars = copy(plugins);
		jars.add(new RuntimePermission("*"));
		jars.add(new MBeanPermission("*", "*"));
		jars.add(new ManagementPermission("monitor"));
		jars.add(new ManagementPermission("control"));
		jars.add(new MBeanServerPermission("*"));
		jars.add(new MBeanTrustPermission("register"));
		jars.add(new LoggingPermission("control", ""));
		addJavaPath(System.getProperty("jdk.home"));
		addJavaPath(System.getProperty("java.home"));
		addJavaPath(System.getenv("JAVA_HOME"));
		addPath(System.getProperty("java.library.path"));
		addPath(System.getenv("PATH"));
	}

	private Collection<?> getSystemPropertyValues() {
		Properties properties = System.getProperties();
		synchronized (properties) {
			return (List<?>) new ArrayList<Object>(properties.values());
		}
	}

	private Permissions copy(PermissionCollection perm) {
		Permissions ret = new Permissions();
		Enumeration<Permission> elements = perm.elements();
		while (elements.hasMoreElements()) {
			ret.add(elements.nextElement());
		}
		return ret;
	}

	private void addClassPath(String... paths) {
		Set<String> visited = new HashSet<String>();
		for (String path : paths) {
			if (path == null)
				continue;
			for (String dir : path.split(File.pathSeparator)) {
				addReadableDirectory(new File(dir), visited);
			}
		}
	}

	private void addReadableDirectory(File file, Set<String> visited) {
		String abs = file.getAbsolutePath();
		plugins.add(new FilePermission(abs, "read"));
		logger.debug("FilePermission {} read", abs);
		if (file.isDirectory()) {
			abs = abs + File.separatorChar + "-";
			plugins.add(new FilePermission(abs, "read"));
			logger.debug("FilePermission {} read", abs);
		}
	}

	private void addWritableDirectories(File... directories) {
		for (File dir : directories) {
			addWriteableDirectory(dir);
			try {
				writableLocations.add(url(dir));
			} catch (MalformedURLException e) {
				// skip directory
			}
		}
		try {
			File tmp =new  File(System.getProperty("java.io.tmpdir"));
			addWriteableDirectory(tmp);
			writableLocations.add(url(tmp));
		} catch (IOException e) {
			// can't write to tmp
		}
	}

	private String url(File dir) throws MalformedURLException {
		return dir.toURI().toURL().toExternalForm();
	}

	private void addWriteableDirectory(File dir) {
		String path = dir.getAbsolutePath();
		plugins.add(new FilePermission(path, "read"));
		plugins.add(new FilePermission(path, "write"));
		logger.debug("FilePermission {} read write", path);
		path = path + File.separatorChar + "-";
		plugins.add(new FilePermission(path, "read"));
		plugins.add(new FilePermission(path, "write"));
		plugins.add(new FilePermission(path, "delete"));
		logger.debug("FilePermission {} read write delete", path);
	}

	private void addJavaPath(String path) {
		if (path != null) {
			File parent = new File(path).getParentFile();
			addPath(parent.getAbsolutePath());
		}
	}

	private void addPath(String... paths) {
		for (String path : paths) {
			if (path == null)
				continue;
			for (String dir : path.split(File.pathSeparator)) {
				String file = new File(dir).getAbsolutePath();
				jars.add(new FilePermission(file, "read"));
				logger.debug("FilePermission {} read from jars", file);
				file = file + File.separatorChar + "-";
				jars.add(new FilePermission(file, "read"));
				jars.add(new FilePermission(file, "execute"));
				logger.debug("FilePermission {} read execute from jars", file);
			}
		}
	}

}
