package pl.miloszgilga.event.proxy.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

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

  public static String generateSha256(String key) {
    try {
      final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = sha256.digest(key.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("SHA-256 not available", ex);
    }
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
