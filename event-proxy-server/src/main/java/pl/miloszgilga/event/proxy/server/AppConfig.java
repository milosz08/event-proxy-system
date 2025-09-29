package pl.miloszgilga.event.proxy.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
  private static final String PROPERTIES_FILE = "server.properties";

  private final Properties properties;

  public AppConfig() {
    properties = new Properties();
    try (final InputStream input = new FileInputStream(PROPERTIES_FILE)) {
      properties.load(input);
    } catch (IOException ex) {
      throw new RuntimeException(String.format("unable to load %s file", PROPERTIES_FILE), ex);
    }
  }

  public String getAsStr(AppConfig.Prop prop) {
    return properties.getProperty(prop.key);
  }

  public long getAsLong(AppConfig.Prop prop) {
    return Utils.safetyParseLong(properties.getProperty(prop.key), 0L);
  }

  public int getAsInt(AppConfig.Prop prop) {
    return Utils.safetyParseInt(properties.getProperty(prop.key), 0);
  }

  public enum Prop {
    HTTP_PORT("http-port"),
    SSE_HEARTBEAT_INTERVAL_SEC("sse-heartbeat-interval-sec"),
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
