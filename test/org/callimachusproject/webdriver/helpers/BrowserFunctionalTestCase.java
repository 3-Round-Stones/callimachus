package org.callimachusproject.webdriver.helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.callimachusproject.Version;
import org.callimachusproject.test.TemporaryServer;
import org.callimachusproject.test.TemporaryServerFactory;
import org.callimachusproject.test.WebResource;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.callimachusproject.webdriver.pages.CalliPage;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openrdf.OpenRDFException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BrowserFunctionalTestCase extends TestCase {
	private static final char DELIM1 = ' ';
	private static final char DELIM2 = '*';
	private static final String PASSWORD = "testPassword1";
	private static final String EMAIL = "test@example.com";
	private static final int PORT = 8088;
	protected static final Logger logger = LoggerFactory.getLogger(BrowserFunctionalTestCase.class);
	private static final String HOSTNAME = DomainNameSystemResolver
			.getInstance().getCanonicalLocalHostName();
	private static final String ORIGIN = "http://" + HOSTNAME + ":" + PORT;
	private static final TemporaryServer server;
	private static final Map<String, RemoteWebDriverFactory> factories = new LinkedHashMap<String, RemoteWebDriverFactory>();
	static {
		String service = System
				.getProperty("org.callimachusproject.test.service");
		if (service == null || service.length() == 0) {
			String email = System.getProperty("org.callimachusproject.test.email");
			if (email == null || email.length() == 0) {
				email = EMAIL;
			}
			String password = System.getProperty("org.callimachusproject.test.password");
			if (password == null || password.length() == 0) {
				password = PASSWORD;
			}
			server = new TemporaryServerFactory(ORIGIN, PORT, email, password.toCharArray())
					.createServer();
		} else {
			server = null;
		}
		String remotewebdriver = System
				.getProperty("org.callimachusproject.test.remotewebdriver");
		try {
			final URL url = remotewebdriver == null
					|| remotewebdriver.length() == 0 ? null : new URL(
					remotewebdriver);
			if (url == null) {
				checkAndStore("chrome", new RemoteWebDriverFactory() {
					public RemoteWebDriver create(String name) {
						return new ChromeDriver();
					}
				});
				checkAndStore("firefox", new RemoteWebDriverFactory() {
					public RemoteWebDriver create(String name) {
						return new FirefoxDriver();
					}
				});
				checkAndStore("ie", new RemoteWebDriverFactory() {
					public RemoteWebDriver create(String name) {
						return new InternetExplorerDriver();
					}
				});
			} else {
				factories.put("chrome", new RemoteWebDriverFactory() {
					public RemoteWebDriver create(String name) {
						DesiredCapabilities caps = DesiredCapabilities.chrome();
						caps.setVersion("27");
						caps.setPlatform(Platform.ANY);
						caps.setCapability("name", name);
						caps.setCapability("build", Version.getInstance().getVersion());
						return new RemoteWebDriver(url, caps);
					}
				});
				factories.put("firefox", new RemoteWebDriverFactory() {
					public RemoteWebDriver create(String name) {
						DesiredCapabilities caps = DesiredCapabilities
								.firefox();
						caps.setVersion("21");
						caps.setPlatform(Platform.ANY);
						caps.setCapability("name", name);
						caps.setCapability("build", Version.getInstance().getVersion());
						return new RemoteWebDriver(url, caps);
					}
				});
				factories.put("ie", new RemoteWebDriverFactory() {
					public RemoteWebDriver create(String name) {
						DesiredCapabilities caps = DesiredCapabilities
								.internetExplorer();
						caps.setVersion("9");
						caps.setCapability("platform", "Windows 7");
						caps.setCapability("name", name);
						caps.setCapability("build", Version.getInstance().getVersion());
						return new RemoteWebDriver(url, caps);
					}
				});
			}
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		}
	}

	public static TestSuite suite() throws Exception {
		return new TestSuite();
	}

	private static void checkAndStore(String browser,
			RemoteWebDriverFactory supplier) {
		try {
			supplier.create("availability").quit();
			factories.put(browser, supplier);
		} catch (IllegalStateException e) {
			logger.warn("Local {} web driver not available", browser);
		} catch (WebDriverException e) {
			logger.warn("Remote {} web driver not available", browser);
		}
	}

	public static TestSuite suite(
			Class<? extends BrowserFunctionalTestCase> testcase)
			throws Exception {
		return suite(testcase, Collections.singleton(""));
	}

	public static TestSuite suite(
			Class<? extends BrowserFunctionalTestCase> testcase,
			Collection<String> variations) throws Exception {
		TestSuite suite = new TestSuite(testcase.getName());
		for (Method method : testcase.getMethods()) {
			if (method.getName().startsWith("test")
					&& method.getParameterTypes().length == 0
					&& method.getReturnType().equals(Void.TYPE)) {
				for (String name : variations) {
					for (Map.Entry<String, RemoteWebDriverFactory> e : getInstalledWebDrivers()
							.entrySet()) {
						String browser = e.getKey();
						RemoteWebDriverFactory supplier = e.getValue();
						BrowserFunctionalTestCase test = testcase.newInstance();
						test.setName(method.getName() + DELIM1 + name + DELIM2
								+ browser);
						test.setRemoteWebDriverFactory(supplier);
						suite.addTest(test);
					}
				}
			}
		}
		if (suite.countTestCases() == 0) {
			suite.addTest(TestSuite.warning(testcase.getName()
					+ " has no public test methods"));
		}
		return suite;
	}

	private static Map<String, RemoteWebDriverFactory> getInstalledWebDrivers() {
		return factories;
	}

	private RemoteWebDriverFactory driverFactory;
	private RemoteWebDriver driver;
	protected CalliPage page;

	public BrowserFunctionalTestCase() {
		super();
	}

	public BrowserFunctionalTestCase(String variation, BrowserFunctionalTestCase parent) {
		super("test" + DELIM1 + variation + DELIM2 + parent.driver.getCapabilities().getBrowserName());
		this.driver = parent.driver;
	}

	public void setRemoteWebDriverFactory(RemoteWebDriverFactory factory) {
		this.driverFactory = factory;
	}

	public String getUsername() {
		if (server == null) {
			return System.getProperty("org.callimachusproject.test.username");
		} else {
			return server.getUsername();
		}
	}

	public char[] getPassword() {
		if (server == null) {
			String password = System.getProperty("org.callimachusproject.test.password");
			if (password == null) {
				return null;
			} else {
				return password.toCharArray();
			}
		} else {
			return server.getPassword();
		}
	}

	public String getVariation() {
		String name = getName();
		return name.substring(name.indexOf(DELIM1) + 1,
				name.lastIndexOf(DELIM2));
	}

	@Override
	public void setUp() throws Exception {
		if (server != null) {
			server.resume();
		}
		String url = getStartUrl();
		WebResource home = new WebResource(url);
		home.get("text/html");
		home.ref("/callimachus/scripts.js").get("text/javascript");
		home.ref("/callimachus/1.0/styles/callimachus.less?less").get(
				"text/css");
		if (driver == null) {
			if (driverFactory == null) {
				driverFactory = getInstalledWebDrivers().get(getBrowserName());
			}
			driver = driverFactory.create(getName());
			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			driver.manage().timeouts().setScriptTimeout(30, TimeUnit.SECONDS);
			driver.navigate().to(url);
		}
		page = new CalliPage(new WebBrowserDriver(driver));
	}

	private String getStartUrl() throws OpenRDFException {
		if (server == null) {
			return System.getProperty("org.callimachusproject.test.service");
		} else {
			return server.getRepository().getCallimachusUrl(server.getOrigin(), "/");
		}
	}

	@Override
	public void tearDown() throws Exception {
		driver.quit();
		driver = null;
		if (server != null) {
			server.pause();
		}
	}

	@Override
	protected void runTest() throws Throwable {
		Method runMethod = null;
		try {
			runMethod = this.getClass().getMethod(getMethodName(),
					(Class[]) null);
		} catch (NoSuchMethodException e) {
			fail("Method \"" + getMethodName() + "\" not found");
		}
		if (!Modifier.isPublic(runMethod.getModifiers())) {
			fail("Method \"" + getMethodName() + "\" should be public");
		}

		try {
			runMethod.invoke(this, (Object[]) new Class[0]);
		} catch (InvocationTargetException e) {
			e.fillInStackTrace();
			throw e.getTargetException();
		} catch (IllegalAccessException e) {
			e.fillInStackTrace();
			throw e;
		}
	}

	private String getMethodName() {
		String name = getName();
		return name.substring(0, name.indexOf(DELIM1));
	}

	private String getBrowserName() {
		String name = getName();
		return name.substring(name.lastIndexOf(DELIM2) + 1);
	}
}
