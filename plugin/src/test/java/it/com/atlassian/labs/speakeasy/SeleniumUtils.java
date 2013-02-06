package it.com.atlassian.labs.speakeasy;

import org.openqa.selenium.WebElement;

import java.lang.reflect.InvocationTargetException;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.2
 */
public class SeleniumUtils {

    public static boolean isDisplayed(WebElement webElement)
    {
        try {
            return (Boolean) webElement.getClass().getMethod("isDisplayed").invoke(webElement);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
