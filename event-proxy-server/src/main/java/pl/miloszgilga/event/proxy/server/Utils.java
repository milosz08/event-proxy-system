package pl.miloszgilga.event.proxy.server;

import java.security.SecureRandom;

public class Utils {
  private static final String ALPHANUMERIC_CHARACTERS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  public static String generateSecurePassword(int length) {
    final StringBuilder password = new StringBuilder(length);
    final SecureRandom secureRandom = new SecureRandom();
    for (int i = 0; i < length; i++) {
      final int randomIndex = secureRandom.nextInt(ALPHANUMERIC_CHARACTERS.length());
      password.append(ALPHANUMERIC_CHARACTERS.charAt(randomIndex));
    }
    return password.toString();
  }

  public static int safetyParseInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }

  public static long safetyParseLong(String value, long defaultValue) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }
}
