package pl.miloszgilga.event.proxy.server;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

class I18n {
  private static final String BUNDLE_BASE_NAME = "i18n.messages";

  String getMessage(String key, Locale locale) {
    try {
      final ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
      return resourceBundle.getString(key);
    } catch (MissingResourceException ignored) {
      return key;
    }
  }
}
