package pl.miloszgilga.event.desktop.client.gui;

import io.reactivex.rxjava3.core.Observable;
import pl.miloszgilga.event.desktop.client.controller.ProxyServerConnectionController;
import pl.miloszgilga.event.desktop.client.state.AppRootState;
import pl.miloszgilga.event.desktop.client.state.ProxyServerData;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ProxyServerConnectionPanel extends JPanel implements ObservableComponent {
  private final AppRootState appRootState;
  private final ProxyServerConnectionController controller;

  private final JComboBox<ProxyServerData> selectedProxyServer;
  private final JButton connectButton;
  private final JButton disconnectButton;
  private final JButton addProxyServerButton;
  private final JButton showAllProxyServersButton;

  ProxyServerConnectionPanel(MainWindow mainWindow) {
    appRootState = mainWindow.getAppRootState();
    controller = new ProxyServerConnectionController(mainWindow, this);

    setLayout(new GridLayout(1, 0));

    selectedProxyServer = new JComboBox<>();
    selectedProxyServer.setRenderer(new ProxyServersComboBoxRenderer());
    connectButton = new JButton("Connect");
    disconnectButton = new JButton("Disconnect");
    addProxyServerButton = new JButton("Add proxy server");
    showAllProxyServersButton = new JButton("Show all proxy servers");

    connectButton.addActionListener(e -> controller.onConnect());
    disconnectButton.addActionListener(e -> controller.onDisconnect());
    addProxyServerButton.addActionListener(e -> controller.onAddProxyServer());
    showAllProxyServersButton.addActionListener(e -> controller.onShowAllProxyServers());

    initObservables();

    add(selectedProxyServer);
    add(connectButton);
    add(disconnectButton);
    add(addProxyServerButton);
    add(showAllProxyServersButton);
  }

  @Override
  public void initObservables() {
    final Observable<ProxyServersStateInfo> stateInfoObservable = Observable.combineLatest(
      appRootState.getPersistedProxyServers$(),
      appRootState.getConnectedProxyServerData$(),
      (servers, server) -> new ProxyServersStateInfo(!servers.isEmpty(), server.isPresent())
    );
    appRootState.wrapAsDisposable(stateInfoObservable, stateInfo -> {
      selectedProxyServer.setEnabled(!stateInfo.connected);
      connectButton.setEnabled(!stateInfo.connected && stateInfo.hasPersistedProxyServers);
      disconnectButton.setEnabled(stateInfo.connected);
      addProxyServerButton.setEnabled(!stateInfo.connected);
      showAllProxyServersButton.setEnabled(!stateInfo.connected);
    });
    appRootState.wrapAsDisposable(appRootState.getPersistedProxyServers$(), proxyServers ->
      proxyServers.forEach(selectedProxyServer::addItem));
  }

  public JComboBox<ProxyServerData> getSelectedProxyServer() {
    return selectedProxyServer;
  }

  private static class ProxyServersComboBoxRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value == null) {
        setText("No proxy servers found");
      }
      return this;
    }
  }

  private record ProxyServersStateInfo(boolean hasPersistedProxyServers, boolean connected) {
  }
}
