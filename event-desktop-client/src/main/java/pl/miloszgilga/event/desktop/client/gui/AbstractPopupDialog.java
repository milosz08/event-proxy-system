package pl.miloszgilga.event.desktop.client.gui;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractPopupDialog extends JDialog {
  private final JFrame mainWindowFrame;
  private final String title;
  private final JPanel panel;
  private final Dimension size;

  protected AbstractPopupDialog(JFrame mainWindowFrame, String title, int width, int height) {
    this.mainWindowFrame = mainWindowFrame;
    this.title = title;
    panel = new JPanel();
    size = new Dimension(width, height);
  }

  protected void init() {
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    panel.setLayout(new BorderLayout(10, 10));

    setModal(true);
    setSize(size);
    setMaximumSize(size);
    setMinimumSize(size);
    setLocationRelativeTo(mainWindowFrame);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setResizable(false);
    setTitle(title);
    extendsDialog(this, panel);
    add(panel);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (!visible) {
      dispose();
    }
  }

  protected abstract void extendsDialog(JDialog dialog, JPanel rootPanel);
}
