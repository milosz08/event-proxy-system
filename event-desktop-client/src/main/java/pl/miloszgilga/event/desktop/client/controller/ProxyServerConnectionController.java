package pl.miloszgilga.event.desktop.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.desktop.client.gui.MainWindow;
import pl.miloszgilga.event.desktop.client.gui.ProxyServerConnectionPanel;
import pl.miloszgilga.event.desktop.client.state.ProxyServerData;

import javax.swing.*;

public class ProxyServerConnectionController {
  private static final Logger LOG = LoggerFactory.getLogger(ProxyServerConnectionController.class);

  private final MainWindow mainWindow;
  private final ProxyServerConnectionPanel panel;

  public ProxyServerConnectionController(MainWindow mainWindow, ProxyServerConnectionPanel panel) {
    this.mainWindow = mainWindow;
    this.panel = panel;
  }

  public void onConnect() {
    final ProxyServerData selectedProxyServer = getSelectedProxyServer();
    if (selectedProxyServer != null) {
      // TODO: connect to server (login)
      LOG.info("Connected with proxy server: {}", selectedProxyServer);
      mainWindow.getAppRootState().updateConnectedProxyServerData(selectedProxyServer);
    }
  }

  public void onDisconnect() {
    final ProxyServerData selectedProxyServer = getSelectedProxyServer();
    if (selectedProxyServer != null) {
      // TODO: disconnected from server (logout)
      LOG.info("Disconnected with proxy server: {}", selectedProxyServer);
      mainWindow.getAppRootState().updateConnectedProxyServerData(null);
    }
  }

  public void onAddProxyServer() {
    mainWindow.getAddProxyServerDialog().setVisible(true);
  }

  public void onShowAllProxyServers() {
    mainWindow.getListProxyServersDialog().setVisible(true);
  }

  private ProxyServerData getSelectedProxyServer() {
    return (ProxyServerData) panel.getSelectedProxyServer().getSelectedItem();
  }
}
