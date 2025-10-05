package pl.miloszgilga.event.desktop.client.gui;

import pl.miloszgilga.event.desktop.client.state.AbstractDisposableProvider;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class MainWindowAdapter extends WindowAdapter {
  private final JFrame frame;
  private final AbstractDisposableProvider disposableProvider;

  MainWindowAdapter(JFrame frame, AbstractDisposableProvider disposableProvider) {
    this.frame = frame;
    this.disposableProvider = disposableProvider;
  }

  @Override
  public void windowClosing(WindowEvent e) {
    final int result = JOptionPane.showConfirmDialog(frame, "Are you sure to close app?",
      "Please confirm", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    if (result == JOptionPane.YES_OPTION) {
      disposableProvider.disposeAllSubscriptions();
      System.exit(0);
    }
  }
}
