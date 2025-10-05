package pl.miloszgilga.event.desktop.client.state;

import io.reactivex.rxjava3.annotations.NonNull;

public record ProxyServerData(String name, String ipOrDomain, int port) {
  public ProxyServerData(String name, String ipOrDomain) {
    this(name, ipOrDomain, 443);
  }

  @NonNull
  @Override
  public String toString() {
    return String.format("%s (%s:%d)", name, ipOrDomain, port);
  }
}
