package pl.miloszgilga.event.desktop.client.gui;

import javax.swing.*;

public class ListProxyServersDialog extends AbstractPopupDialog {
  protected ListProxyServersDialog(JFrame mainWindowFrame) {
    super(mainWindowFrame, "Persisted proxy servers", 500, 300);
  }

  @Override
  protected void extendsDialog(JDialog dialog, JPanel rootPanel) {
  }
}
