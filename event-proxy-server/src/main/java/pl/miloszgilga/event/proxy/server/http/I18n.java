package pl.miloszgilga.event.proxy.server.http;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18n {
  private static final String BUNDLE_BASE_NAME = "i18n.messages";

  public String getMessage(String key, Locale locale) {
    try {
      final ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
      return resourceBundle.getString(key);
    } catch (MissingResourceException ignored) {
      return key;
    }
  }
}
