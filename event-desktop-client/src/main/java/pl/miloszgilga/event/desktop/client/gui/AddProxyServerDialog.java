package pl.miloszgilga.event.desktop.client.gui;

import javax.swing.*;

public class AddProxyServerDialog extends AbstractPopupDialog {
  protected AddProxyServerDialog(JFrame mainWindowFrame) {
    super(mainWindowFrame, "Add proxy server", 400, 250);
  }

  @Override
  protected void extendsDialog(JDialog dialog, JPanel rootPanel) {
  }
}
