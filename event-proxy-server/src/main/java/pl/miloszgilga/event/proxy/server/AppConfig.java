package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
  private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
  private static final String PROPERTIES_FILE = "server.properties";

  private final Properties properties;

  public AppConfig() {
    properties = new Properties();
    try (final InputStream input = new FileInputStream(PROPERTIES_FILE)) {
      properties.load(input);
    } catch (IOException ex) {
      LOG.warn("Unable to load file: {}. Configuration will rely solely on environment variables.",
        PROPERTIES_FILE);
    }
  }

  private String resolveProperty(AppConfig.Prop prop) {
    final String envKey = prop.name();
    final String envValue = System.getenv(envKey);

    if (envValue != null && !envValue.isEmpty()) {
      return envValue;
    }
    final String propKey = prop.key;
    final String propValue = properties.getProperty(propKey);

    if (propValue == null) {
      LOG.warn("Configuration key not found either in environment variables (as: {}) or in the " +
        "properties file (as: {}).", envKey, propKey);
    }
    return propValue;
  }

  public String getAsStr(AppConfig.Prop prop) {
    return resolveProperty(prop);
  }

  public long getAsLong(AppConfig.Prop prop) {
    return Utils.safetyParseLong(resolveProperty(prop), 0L);
  }

  public int getAsInt(AppConfig.Prop prop) {
    return Utils.safetyParseInt(resolveProperty(prop), 0);
  }

  // prop.name() -> key to environment path (ex. HTTP_PORT)
  // prop.key -> key to property in properties file (ex. http-port)
  public enum Prop {
    HTTP_PORT("http-port"),
    SSE_HEARTBEAT_INTERVAL_SEC("sse-heartbeat-interval-sec"),
    SSE_HANDSHAKE_PENDING_SEC("sse-handshake-pending-sec"),
    SMTP_PORT("smtp-port"),
    SMTP_THREAD_POOL_SIZE("smtp-thread-pool-size"),
    SMTP_QUEUE_CAPACITY("smtp-queue-capacity"),
    SMTP_SENDER_SUFFIX("smtp-sender-suffix"),
    DB_PATH("db-path"),
    DB_POOL_SIZE("db-pool-size"),
    SESSION_TTL_SEC("session-ttl-sec"),
    SESSION_CLEAR_INTERVAL_SEC("session-clear-interval-sec"),
    ACCOUNT_USERNAME("account-username"),
    ACCOUNT_PASSWORD_LENGTH("account-password-length"),
    ACCOUNT_PASSWORD_HASH_STRENGTH("account-password-hash-strength"),
    ;

    private final String key;

    Prop(String key) {
      this.key = key;
    }
  }
}
