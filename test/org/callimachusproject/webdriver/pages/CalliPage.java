package org.callimachusproject.webdriver.pages;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class CalliPage {
	protected WebBrowserDriver browser;

	public CalliPage(WebBrowserDriver browser) {
		this.browser = browser;
	}

	public String toString() {
		return browser.toString();
	}

	public CalliPage waitUntilTitle(String title) {
		browser.focusInTopWindow();
		browser.waitUntilTextPresent(By.tagName("h1"), title);
		return this;
	}

	public Login openLogin() {
		browser.focusInTopWindow();
		browser.click(By.id("login-link"));
		return page(SignIn.class).loginWithDigest();
	}

	public CalliPage logout() {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("button.navbar-btn.dropdown-toggle"));
		browser.click(By.id("logout-link"));
		return page();
	}
	
	public String getCurrentUrl() {
		String url = browser.getCurrentUrl();
		return url;
	}

	public FolderView openHomeFolder() {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("button.navbar-btn.dropdown-toggle"));
		browser.click(By.linkText("Home folder"));
		return page(FolderView.class);
	}

	public FolderView openCurrentFolder() {
		browser.focusInTopWindow();
		browser.navigateTo("./?view");
		return page(FolderView.class);
	}

	public CalliPage open(String ref) {
		browser.focusInTopWindow();
		browser.navigateTo(ref);
		return page();
	}

	public <P> P open(String ref, Class<P> pageClass) {
		browser.focusInTopWindow();
		browser.navigateTo(ref);
		return page(pageClass);
	}

	public CalliPage openView() {
		browser.focusInTopWindow();
		browser.click(By.linkText("View"));
		return page();
	}

	public <P> P openEdit(Class<P> pageClass) {
		browser.focusInTopWindow();
		browser.click(By.linkText("Edit"));
		return page(pageClass);
	}

	public HistoryPage openHistory() {
		browser.focusInTopWindow();
		browser.click(By.linkText("History"));
		return page(HistoryPage.class);
	}

	public DiscussionPage openDiscussion() {
		browser.focusInTopWindow();
		browser.click(By.linkText("Discussion"));
		return page(DiscussionPage.class);
	}

	public DescribePage openDescribe() {
		browser.focusInTopWindow();
		browser.click(By.linkText("Describe"));
		return page(DescribePage.class);
	}

	public RecentChanges openRecentChanges() {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("button.navbar-btn.dropdown-toggle"));
		browser.click(By.linkText("Recent changes"));
		return page(RecentChanges.class);
	}

	public RecentChanges openRelatedChanges() {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("button.navbar-btn.dropdown-toggle"));
		browser.click(By.linkText("Related changes"));
		return page(RecentChanges.class);
	}

	public SearchResults openWhatLinksHere() {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("button.navbar-btn.dropdown-toggle"));
		browser.click(By.linkText("What links here"));
		return page(SearchResults.class);
	}

	public CalliPage openProfile() {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("button.navbar-btn.dropdown-toggle"));
		browser.click(By.id("profile-link"));
		return page();
	
	}

	public SearchResults searchFor(String conceptLabel) {
		browser.focusInTopWindow();
		browser.type(By.xpath("//input[@name='q']"), conceptLabel);
		browser.submit(By.xpath("//input[@name='q']"));
		return page(SearchResults.class);
	}

	public CalliPage back() {
		browser.navigateBack();
		return page();
	}

	public CalliPage page() {
		browser.waitForScript();
		browser.focusInTopWindow();
		return new CalliPage(browser);
	}

	public <P> P page(Class<P> pageClass) {
		browser.waitForScript();
		browser.focusInTopWindow();
		try {
			try {
				return pageClass.getConstructor(WebBrowserDriver.class).newInstance(browser);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		} catch (Error e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new UndeclaredThrowableException(e);
		}
	}

}
