package org.callimachusproject.installer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class SetupProxy {
	private final Class<?> Setup;
	private final Object setup;

	public SetupProxy(ClassLoader cl) throws ClassNotFoundException {
		// FIXME don't need this for alibaba-2.0-rc-2
		Thread.currentThread().setContextClassLoader(cl);
		this.Setup = Class.forName("org.callimachusproject.Setup", true, cl);
		try {
			this.setup = Setup.newInstance();
		} catch (InstantiationException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	public SetupProxy(File lib) throws MalformedURLException,
			ClassNotFoundException {
		this(new URLClassLoader(list(lib)));
	}

	private static URL[] list(File lib) throws MalformedURLException {
		File[] list = lib.listFiles();
		if (list == null)
			throw new IllegalArgumentException("Directory is empty: "
					+ lib.getAbsolutePath());
		URL[] urls = new URL[list.length];
		for (int i = 0; i < list.length; i++) {
			urls[i] = list[i].toURI().toURL();
		}
		return urls;
	}

	public File connect(File dir, String config) throws Exception {
		try {
			Method connect = Setup.getMethod("connect", File.class, String.class);
			return (File) connect.invoke(setup, dir, config);
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

	public void disconnect() {
		try {
			Method disconnect = Setup.getMethod("disconnect");
			disconnect.invoke(setup);
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
			} catch (Throwable exc) {
				throw new AssertionError(e);
			}
		}
	}

	public void createOrigin(String origin, URL car) throws Exception {
		try {
			Method createOrigin = Setup.getMethod("createOrigin", String.class,
					URL.class);
			createOrigin.invoke(setup, origin, car);
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

	public void createVirtualHost(String virtual, String origin)
			throws Exception {
		try {
			Method createVirtualHost = Setup.getMethod("createVirtualHost",
					String.class, String.class);
			createVirtualHost.invoke(setup, virtual, origin);
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

	public void createRealm(String realm, String origin) throws Exception {
		try {
			Method createRealm = Setup.getMethod("createRealm", String.class,
					String.class);
			createRealm.invoke(setup, realm, origin);
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
