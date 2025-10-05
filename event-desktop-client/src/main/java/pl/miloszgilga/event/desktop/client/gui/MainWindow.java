package pl.miloszgilga.event.desktop.client.gui;

import pl.miloszgilga.event.desktop.client.state.AppRootState;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
  private static final String APP_NAME = "Notifications center";
  private static final int WIDTH = 1280;
  private static final int HEIGHT = 720;

  private final AppRootState appRootState;
  private final JPanel panel;
  private final Dimension size;

  private final AddProxyServerDialog addProxyServerDialog;
  private final ListProxyServersDialog listProxyServersDialog;

  private final ProxyServerConnectionPanel proxyServerConnectionPanel;

  public MainWindow(AppRootState appRootState) {
    this.appRootState = appRootState;
    panel = new JPanel();
    size = new Dimension(WIDTH, HEIGHT);

    addProxyServerDialog = new AddProxyServerDialog(this);
    listProxyServersDialog = new ListProxyServersDialog(this);

    proxyServerConnectionPanel = new ProxyServerConnectionPanel(this);
  }

  public void init() {
    panel.setBounds(0, 0, getWidth(), getHeight());

    setSize(size);
    setMinimumSize(size);
    setLocation(getMotherScreenCenter());
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(new MainWindowAdapter(this, appRootState));
    setTitle(APP_NAME);
    setLayout(new BorderLayout());
    add(panel, BorderLayout.CENTER);

    addProxyServerDialog.init();
    listProxyServersDialog.init();

    add(proxyServerConnectionPanel, BorderLayout.NORTH);
  }

  private Point getMotherScreenCenter() {
    final Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    final int x = dimension.width / 2 - getWidth() / 2;
    final int y = dimension.height / 2 - getHeight() / 2;
    return new Point(x, y);
  }

  public AppRootState getAppRootState() {
    return appRootState;
  }

  public AddProxyServerDialog getAddProxyServerDialog() {
    return addProxyServerDialog;
  }

  public ListProxyServersDialog getListProxyServersDialog() {
    return listProxyServersDialog;
  }
}
