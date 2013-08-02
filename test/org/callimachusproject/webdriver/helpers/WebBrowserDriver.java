package org.callimachusproject.webdriver.helpers;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.callimachusproject.engine.model.TermFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WebBrowserDriver {
	public RemoteWebDriver driver;

	public WebBrowserDriver(RemoteWebDriver driver) {
		this.driver = driver;
	}

	public RemoteWebDriver getRemoteWebDriver() {
		return driver;
	}

	public void quit() {
		driver.quit();
	}

	public void navigateTo(String ref) {
		String url = TermFactory.newInstance(driver.getCurrentUrl()).resolve(ref);
		driver.navigate().to(url);
	    waitForScript();
	}

	public void navigateBack() {
		driver.navigate().back();
		waitForScript();
	}

	public void focusInTopWindow() {
		driver.switchTo().window(driver.getWindowHandle());
	}

	public void focusInFrame(String... frameNames) {
		driver.switchTo().window(driver.getWindowHandle());
		waitForScript();
		for (final String frameName : frameNames) {
			if (frameName != null) {
				new WebDriverWait(driver, 10)
						.until(new ExpectedCondition<WebDriver>() {
							public WebDriver apply(WebDriver driver) {
								try {
									return driver.switchTo().frame(frameName);
								} catch (NoSuchFrameException e) {
									return null;
								}
							}
						});
			}
		}
		waitForScript();
	}

	public void focusInFrame(int... frames) {
		focusInSubFrame(null, frames);
	}

	public void focusInSubFrame(String topFrameName, int... frames) {
		driver.switchTo().window(driver.getWindowHandle());
		waitForScript();
		if (topFrameName != null) {
			driver.switchTo().frame(topFrameName);
		}
		for (final int frame : frames) {
			new WebDriverWait(driver, 10)
					.until(new ExpectedCondition<WebDriver>() {
						public WebDriver apply(WebDriver driver) {
							try {
								return driver.switchTo().frame(frame);
							} catch (NoSuchFrameException e) {
								return null;
							}
						}
					});
		}
		waitForScript();
	}

	public void click(By locator) {
		waitForScript();
		WebElement element = driver.findElement(locator);
		try {
			new Actions(driver).moveToElement(element).build().perform();
			element.click();
		} catch (MoveTargetOutOfBoundsException e) {
			// firefox can't scroll to reveal element
			driver.executeScript("arguments[0].click()", element);
		}
	}

	public void submit(By locator) {
		waitForScript();
		driver.findElement(locator).submit();
	}

	public void type(By locator, String text) {
		waitForScript();
		WebElement element = driver.findElement(locator);
		element.clear();
		sendKeys(element, text);
		// tab to the next element to fire onchange event
		sendKeys(element, Keys.TAB);
	}

	public void confirm(String msg) {
		Alert alert = driver.switchTo().alert();
		assertTrue(alert.getText().contains(msg));
	    alert.accept();
	    waitForScript();
	}

	public void clickHiddenLink(String cssSelector) {
		driver.executeScript("document.querySelector(arguments[0]).click()", cssSelector);
	}

	public void sendKeys(CharSequence... keys) {
		sendKeys(driver.switchTo().activeElement(), keys);
	}

	public void sendKeys(By locator, CharSequence... keys) {
		sendKeys(driver.findElement(locator), keys);
	}

	private void sendKeys(WebElement element, CharSequence... keys) {
		List<CharSequence> list = new ArrayList<CharSequence>(keys.length);
		for (CharSequence key : keys) {
			if (key instanceof String && ((String) key).contains("-")) {
				for (String text : ((String) key).split("-")) {
					list.add(text);
					list.add(Keys.SUBTRACT);
				}
				list.remove(list.size() - 1);
			} else {
				list.add(key);
			}
		}
		element.sendKeys(list.toArray(new CharSequence[list.size()]));
	}

	public void waitUntilElementPresent(final By locator) {
		waitForScript();
		new WebDriverWait(driver, 10).until(ExpectedConditions
				.presenceOfElementLocated(locator));
	}

	public void waitUntilTextPresent(final By locator, final String needle) {
		waitForScript();
		Wait<WebDriver> wait = new WebDriverWait(driver, 30);
		Boolean present = wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				for (WebElement element : driver.findElements(locator)) {
					if (element.getText().contains(needle)) {
						return true;
					}
				}
				return null;
			}
		});
		assertTrue(present);
	}

	public void waitForScript() {
		Wait<WebDriver> wait = new WebDriverWait(driver, 120);
		Boolean present = wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver wd) {
				String className = (String) driver
						.executeScript("if (document.body && window.document.documentElement) return window.document.documentElement.className;\n"
								+ "else if (document.body) return '';\n"
								+ "else return 'wait';");
				if (!className.contains("wait")) {
					return true;
				} else {
					return null;
				}
			}
		});
		assertTrue(present);
	}

	public int getPositionTop(By locator) {
		return driver.findElement(locator).getLocation().getY();
	}

	public int getPositionBottom(By locator) {
		WebElement element = driver.findElement(locator);
		return element.getLocation().getY() + element.getSize().getHeight();
	}

	public String getText(By locator) {
		return driver.findElement(locator).getText();
	}

	public int getElementCount(By locator) {
		return driver.findElements(locator).size();
	}

	public List<String> getTextOfElements(By locator) {
		List<WebElement> elements = driver.findElements(locator);
		List<String> texts = new ArrayList<String>(elements.size());
		for (WebElement element : elements) {
			texts.add(element.getText());
		}
		return texts;
	}

}
