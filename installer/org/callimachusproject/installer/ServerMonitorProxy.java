package org.callimachusproject.installer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ServerMonitorProxy {
	private final Class<?> ServerMonitor;
	private final Object monitor;

	public ServerMonitorProxy(String pidFile, ClassLoader cl) throws ClassNotFoundException, Exception {
		String cname = "org.callimachusproject.ServerMonitor";
		this.ServerMonitor = Class.forName(cname, true, cl);
		try {
			this.monitor = ServerMonitor.getConstructor(String.class).newInstance(pidFile);
		} catch (InstantiationException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (SecurityException e) {
			throw new AssertionError(e);
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (Error e1) {
				throw e1;
			} catch (Exception e1) {
				throw e1;
			} catch (Throwable e1) {
				throw new AssertionError(e);
			}
		}
	}

	public ServerMonitorProxy(String pidFile, File lib) throws MalformedURLException, ClassNotFoundException, Exception {
		this(pidFile, new URLClassLoader(list(lib)));
	}

	private static URL[] list(File lib) throws MalformedURLException {
		File[] list = lib.listFiles();
		if (list == null)
			throw new IllegalArgumentException("Directory is empty: " + lib.getAbsolutePath());
		URL[] urls = new URL[list.length];
		for (int i=0; i<list.length; i++) {
			urls[i] = list[i].toURI().toURL();
		}
		return urls;
	}

	public void logNotifications(String log) throws Exception {
		try {
			Method logNotifications = ServerMonitor.getMethod("logNotifications", String.class);
			logNotifications.invoke(monitor, log);
		} catch (SecurityException e) {
			throw new AssertionError(e);
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (Error exc) {
				throw exc;
			} catch (RuntimeException exc) {
				throw exc;
			} catch (Exception exc) {
				throw exc;
			} catch (Throwable exc) {
				throw new AssertionError(e);
			}
		}
	}

	public void destroyService() throws Exception {
		try {
			Method destroyService = ServerMonitor.getMethod("destroyService");
			destroyService.invoke(monitor);
		} catch (SecurityException e) {
			throw new AssertionError(e);
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (Error exc) {
				throw exc;
			} catch (RuntimeException exc) {
				throw exc;
			} catch (Exception exc) {
				throw exc;
			} catch (Throwable exc) {
				throw new AssertionError(e);
			}
		}
	}

	public void resetCache() throws Exception {
		try {
			Method resetCache = ServerMonitor.getMethod("resetCache");
			resetCache.invoke(monitor);
		} catch (SecurityException e) {
			throw new AssertionError(e);
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (Error exc) {
				throw exc;
			} catch (RuntimeException exc) {
				throw exc;
			} catch (Exception exc) {
				throw exc;
			} catch (Throwable exc) {
				throw new AssertionError(e);
			}
		}
	}

	public void dumpService(String dir) throws Exception {
		try {
			Method dumpService = ServerMonitor.getMethod("dumpService", String.class);
			dumpService.invoke(monitor, dir);
		} catch (SecurityException e) {
			throw new AssertionError(e);
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (Error exc) {
				throw exc;
			} catch (RuntimeException exc) {
				throw exc;
			} catch (Exception exc) {
				throw exc;
			} catch (Throwable exc) {
				throw new AssertionError(e);
			}
		}
	}

}
