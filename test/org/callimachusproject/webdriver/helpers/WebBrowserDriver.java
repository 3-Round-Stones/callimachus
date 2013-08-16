package org.callimachusproject.webdriver.helpers;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.callimachusproject.engine.model.TermFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
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
	
	public String getCurrentUrl() {
		String url = driver.getCurrentUrl();
		return url;
	}

	public void focusInTopWindow() {
		driver.switchTo().window(driver.getWindowHandle());
	}

	public void focusInFrame(String... frameNames) {
		driver.switchTo().window(driver.getWindowHandle());
		waitForScript();
		for (final String frameName : frameNames) {
			if (frameName != null) {
				driver.switchTo().frame(driver.findElement(By.name(frameName)));
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
			driver.switchTo().frame(driver.findElement(By.name(topFrameName)));
		}
		for (final int frame : frames) {
			new WebDriverWait(driver, 60)
					.until(new ExpectedCondition<WebDriver>() {
						public WebDriver apply(WebDriver driver) {
							try {
								return driver.switchTo().frame(frame);
							} catch (NoSuchFrameException e) {
								return null;
							}
						}

						public String toString() {
							return "frame index " + frame + " to be present";
						}
					});
		}
		waitForScript();
	}

	public void waitForFrameToClose(final String frameName) {
		driver.switchTo().window(driver.getWindowHandle());
		if (frameName != null) {
			try {
				driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
				new WebDriverWait(driver, 60)
						.until(new ExpectedCondition<Boolean>() {
							public Boolean apply(WebDriver driver) {
								return driver.findElements(By.name(frameName))
										.isEmpty();
							}

							public String toString() {
								return "frame " + frameName + " to be absent";
							}
						});
			} finally {
				driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			}
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
		} catch (ElementNotVisibleException e) {
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

	public void clickHiddenLink(final String cssSelector) {
		Wait<WebDriver> wait = new WebDriverWait(driver, 120);
		Boolean present = wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver wd) {
				String js = "return document.querySelector(arguments[0]) && true || false";
				try {
					return (Boolean) driver.executeScript(js, cssSelector);
				} catch (WebDriverException e) {
					return null;
				}
			}

			public String toString() {
				return "hidden " + cssSelector + " to load";
			}
		});
		assertTrue(present);
		driver.executeScript("document.querySelector(arguments[0]).click()",
				cssSelector);
	}
	
	public void select(By locator, String value) {
		List<WebElement> optionList = driver.findElement(locator).findElements(By.tagName("option"));
		for (WebElement option : optionList) {
			if (value.equalsIgnoreCase(option.getText())){
				option.click();
				break;
			}
		}
	}

	public void sendKeys(CharSequence... keys) {
		sendKeys(driver.switchTo().activeElement(), keys);
	}

	public void sendKeys(By locator, CharSequence... keys) {
		sendKeys(driver.findElement(locator), keys);
	}

	private void sendKeys(WebElement element, CharSequence... keys) {
		StringBuilder sb = new StringBuilder();
		List<CharSequence> list = new ArrayList<CharSequence>(keys.length * 2);
		for (CharSequence key : keys) {
			if (key instanceof String) {
				for (char chr : ((String) key).toCharArray()) {
					switch (chr) {
					case '-':
						sendKeys(element, list, sb);
						list.add(Keys.SUBTRACT);
						break;
					case '!':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "1"));
						break;
					case '@':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "2"));
						break;
					case '#':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "3"));
						break;
					case '$':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "4"));
						break;
					case '%':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "5"));
						break;
					case '^':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "6"));
						break;
					case '&':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "7"));
						break;
					case '*':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "8"));
						break;
					case '(':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "9"));
						break;
					case ')':
						sendKeys(element, list, sb);
						list.add(Keys.chord(Keys.SHIFT, "0"));
						break;
					case '{':
					case '}':
					case '<':
					case '>':
					case '[':
					case ']':
					case '"':
					case '\'':
					case '\n':
						sendKeys(element, list, sb);
						sb.append(chr);
						sendKeys(element, list, sb);
						break;
					default:
						sb.append(chr);
					}
				}
			} else {
				list.add(key);
			}
		}
		sendKeys(element, list, sb);
	}

	private void sendKeys(WebElement element, List<CharSequence> keys,
			StringBuilder words) {
		if (words.length() > 0) {
			keys.add(words.toString());
			words.setLength(0);
		}
		if (!keys.isEmpty()) {
			element.sendKeys(keys.toArray(new CharSequence[keys.size()]));
			keys.clear();
		}
	}

	public void waitUntilElementPresent(final By locator) {
		waitForScript();
		new WebDriverWait(driver, 10).until(ExpectedConditions
				.presenceOfElementLocated(locator));
	}

	public void waitUntilTextPresent(final By locator, final String needle) {
		waitForScript();
		Wait<WebDriver> wait = new WebDriverWait(driver, 60);
		Boolean present = wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				for (WebElement element : driver.findElements(locator)) {
					try {
						if (element.getText().contains(needle)) {
							return true;
						}
					} catch (StaleElementReferenceException e) {
						continue;
					}
				}
				return null;
			}

			public String toString() {
				return "text " + needle + " to be present in " + locator;
			}
		});
		assertTrue(present);
	}

	public void waitForScript() {
		Wait<WebDriver> wait = new WebDriverWait(driver, 240);
		Boolean present = wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver wd) {
				String js = "try {\n" + "if (document.documentElement)\n"
						+ "    return document.documentElement.className;\n"
						+ "else if (document.body)\n" + "    return '';\n"
						+ "} catch(e) {}\n" + "    return 'wait';";
				try {
					Object className = driver.executeScript(js);
					return !String.valueOf(className).contains("wait");
				} catch (WebDriverException e) {
					return null;
				}
			}

			public String toString() {
				return "script to be ready";
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
		try {
			return driver.findElement(locator).getText();
		} catch (StaleElementReferenceException e) {
			return null;
		}
	}

	public int getElementCount(By locator) {
		return driver.findElements(locator).size();
	}

	public List<String> getTextOfElements(By locator) {
		List<WebElement> elements = driver.findElements(locator);
		List<String> texts = new ArrayList<String>(elements.size());
		for (WebElement element : elements) {
			try {
				texts.add(element.getText());
			} catch (StaleElementReferenceException e) {
				continue;
			}
		}
		return texts;
	}

}
