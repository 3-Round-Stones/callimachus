package org.callimachusproject.webdriver.helpers;

import org.openqa.selenium.remote.RemoteWebDriver;

public interface RemoteWebDriverFactory {

	RemoteWebDriver create(String name);
}
