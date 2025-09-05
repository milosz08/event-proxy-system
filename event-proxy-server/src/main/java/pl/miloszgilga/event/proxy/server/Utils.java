package pl.miloszgilga.event.proxy.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

class Utils {
  static String generateSha256(String key) {
    try {
      final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = sha256.digest(key.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("SHA-256 not available", ex);
    }
  }

  static int safetyParseInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }
}
