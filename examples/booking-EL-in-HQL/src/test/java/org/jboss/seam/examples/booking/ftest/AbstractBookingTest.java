package org.jboss.seam.examples.booking.ftest;

import static org.jboss.test.selenium.guard.request.RequestTypeGuardFactory.waitXhr;
import static org.jboss.test.selenium.locator.LocatorFactory.jq;
import static org.jboss.test.selenium.locator.LocatorFactory.xp;
import static org.testng.AssertJUnit.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.test.selenium.AbstractTestCase;
import org.jboss.test.selenium.locator.JQueryLocator;
import org.jboss.test.selenium.locator.XpathLocator;
import org.testng.annotations.BeforeMethod;


/**
 * Utility methods for the booking example.
 *
 * @author <a href="http://community.jboss.org/people/jharting">Jozef Hartinger</a>
 */
public abstract class AbstractBookingTest extends AbstractTestCase {
    public static final String TITLE = "JBoss Suites: Seam Framework Demo";
    public static final JQueryLocator LOGIN_USERNAME = jq("[id='login:username']");
    public static final JQueryLocator LOGIN_PASSWORD = jq("[id='login:password']");
    public static final JQueryLocator LOGIN_SUBMIT = jq("[id='login:login']");

    public static final JQueryLocator LOGOUT = jq("a:contains('Logout')");
    public static final JQueryLocator MENU_FIND = jq("a:contains('Find a Hotel')");
    public static final JQueryLocator MENU_HOME = jq("a:contains('Home')");
    public static final JQueryLocator MENU_ACCOUNT = jq("a:contains('Account')");
    public static final XpathLocator SEARCH_QUERY = xp("//input[contains(@name,'query')]");

    public static final JQueryLocator SEARCH_NO_RESULTS = jq("#noHotelsMsg");
    public static final JQueryLocator SEARCH_PAGE_SIZE = jq("#pageSize");
    public static final XpathLocator SEARCH_RESULT_TABLE_FIRST_ROW_LINK = xp("//a[contains(@name,'hotelSelectionForm:hotels:0:view')]");


    private final String DEFAULT_USERNAME = "jose";
    private final String DEFAULT_PASSWORD = "brazil";

    @BeforeMethod
    public void setUp(){
        selenium.open(contextPath);
        selenium.waitForPageToLoad();
        if (isLoggedIn()) {
            logout();
        }
        login();
        selenium.click(MENU_FIND);
        selenium.waitForPageToLoad();

    }

    public void login() {
        login(DEFAULT_USERNAME, DEFAULT_PASSWORD);
       
    }

    public void login(String username, String password) {
        selenium.click(MENU_HOME);
        selenium.waitForPageToLoad();
      
        selenium.type(LOGIN_USERNAME, username);
        selenium.type(LOGIN_PASSWORD, password);
        selenium.click(LOGIN_SUBMIT);
        selenium.waitForPageToLoad();

        assertTrue("Login failed.", isLoggedIn());
    }

    public void logout() {
        if (isLoggedIn()) {
        	selenium.click(LOGOUT);
        	selenium.waitForPageToLoad();
        }
    }

    public boolean isLoggedIn() {
        return selenium.isElementPresent(LOGOUT);
    }

    public void enterSearchQuery(String query) {
        selenium.type(SEARCH_QUERY, query);
        waitXhr(selenium).keyUp(SEARCH_QUERY, " ");
    }
  
}
