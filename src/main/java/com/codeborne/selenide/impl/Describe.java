package com.codeborne.selenide.impl;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.*;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.isHtmlUnit;
import static com.codeborne.selenide.WebDriverRunner.supportsJavascript;

public class Describe {
  private WebElement element;
  private StringBuilder sb = new StringBuilder();

  private Describe(WebElement element) {
    this.element = element;
    sb.append('<').append(element.getTagName());
  }

  private Describe appendAttributes() {
    return supportsJavascriptAttributes() ? appendAllAttributes() : appendPredefinedAttributes();
  }

  private Describe appendAllAttributes() {
    Map<String, String> map = executeJavaScript(
        "var s = {};" +
            "var attrs = arguments[0].attributes;" +
            "for (var i = 0; i < attrs.length; i++) {" +
            "   var a = attrs[i]; " +
            "   if (a.name != 'style') {" +
            "     s[a.name] = a.value;" +
            "   }" +
            "}" +
            "return s;", element);
    
    SortedMap<String, String> sortedByName = new TreeMap<>();
    if (map != null) {
      sortedByName.putAll(map);
    }
    sortedByName.put("value", element.getAttribute("value"));
    if (!sortedByName.containsKey("type")) {
      sortedByName.put("type", element.getAttribute("type"));
    }
    
    for (Map.Entry<String, String> entry : sortedByName.entrySet()) {
      attr(entry.getKey(), entry.getValue());
    }
    return this;
  }

  private Describe appendPredefinedAttributes() {
    return attr("class").attr("disabled").attr("href").attr("id").attr("name")
        .attr("onclick").attr("onchange").attr("placeholder")
        .attr("type").attr("value");
  }

  private boolean supportsJavascriptAttributes() {
    return supportsJavascript() && !isHtmlUnit();
  }

  private Describe attr(String attributeName) {
    String attributeValue = element.getAttribute(attributeName);
    return attr(attributeName, attributeValue);
  }

  private Describe attr(String attributeName, String attributeValue) {
    if (attributeValue != null && attributeValue.length() > 0) {
      sb.append(' ').append(attributeName).append("=\"").append(attributeValue).append('"');
    }
    return this;
  }

  private String serialize() {
    String text = element.getText();
    sb.append('>').append(text == null ? "" : text).append("</").append(element.getTagName()).append('>');
    return sb.toString();
  }

  @Override
  public String toString() {
    return sb.toString();
  }

  private String flush() {
    return sb.append('>').toString();
  }

  public static String describe(WebElement element) {
    try {
      if (element == null) {
        return "null";
      }
      return new Describe(element)
          .appendAttributes()
          .isSelected(element)
          .isDisplayed(element)
          .serialize();
    } catch (WebDriverException elementDoesNotExist) {
      return Cleanup.of.webdriverExceptionMessage(elementDoesNotExist);
    }
    catch (IndexOutOfBoundsException e) {
      return e.toString();
    }
  }

  static String shortly(WebElement element) {
    try {
      if (element == null) {
        return "null";
      }
      if (element instanceof SelenideElement) {
        return shortly(((SelenideElement) element).toWebElement());
      }
      return new Describe(element).attr("id").attr("name").flush();
    } catch (WebDriverException elementDoesNotExist) {
      return Cleanup.of.webdriverExceptionMessage(elementDoesNotExist);
    }
    catch (IndexOutOfBoundsException e) {
      return e.toString();
    }
  }

  private Describe isSelected(WebElement element) {
    try {
      if (element.isSelected()) {
        sb.append(' ').append("selected:true");
      }
    } catch (UnsupportedOperationException ignore) {
    } catch (InvalidElementStateException ignore) {
    }
    return this;
  }
  
  private Describe isDisplayed(WebElement element) {
    try {
      if (!element.isDisplayed()) {
        sb.append(' ').append("displayed:false");
      }
    } catch (UnsupportedOperationException e) {
      sb.append(' ').append("displayed:").append(e);
    } catch (InvalidElementStateException e) {
      sb.append(' ').append("displayed:").append(e);
    }
    return this;
  }

  static String shortly(By selector) {
    if (selector instanceof By.ByCssSelector) {
      return selector.toString()
          .replaceFirst("By\\.selector:\\s*(.*)", "$1")
          .replaceFirst("By\\.cssSelector:\\s*(.*)", "$1");
    }
    return selector.toString();
  }

  public static String selector(By selector) {
    return selector.toString()
        .replaceFirst("By\\.selector:\\s*", "")
        .replaceFirst("By\\.cssSelector:\\s*", "");
  }
  
  public static String describe(WebDriver webDriver) {
    return webDriver.getClass().getSimpleName();
  }
}
