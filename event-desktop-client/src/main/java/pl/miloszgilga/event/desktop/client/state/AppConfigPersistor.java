package pl.miloszgilga.event.desktop.client.state;

import java.util.List;

public class AppConfigPersistor {
  private static final String CONFIG_FILE_NAME = "config.dat";

  private final AppRootState appRootState;

  public AppConfigPersistor(AppRootState appRootState) {
    this.appRootState = appRootState;
  }

  public void readConfigFile() {
    final ProxyServerData data = new ProxyServerData("test", "123.456.789");
    appRootState.updatePersistedProxyServers(List.of(data));
  }

  public void listenChanges() {
    appRootState.wrapAsDisposable(appRootState.getPersistedProxyServers$(), servers -> {
      // TODO: persists servers as list into binary file
    });
  }
}
