package pl.miloszgilga.event.desktop.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.desktop.client.gui.MainWindow;
import pl.miloszgilga.event.desktop.client.state.AppConfigPersistor;
import pl.miloszgilga.event.desktop.client.state.AppRootState;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

class EventDesktopClientMain implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(EventDesktopClientMain.class);

  public static void main(String[] args) throws UnsupportedLookAndFeelException {
    final EventDesktopClientMain app = new EventDesktopClientMain();
    app.init();
  }

  void init() throws UnsupportedLookAndFeelException {
    UIManager.setLookAndFeel(new MetalLookAndFeel());
    SwingUtilities.invokeLater(this);
  }

  @Override
  public void run() {
    LOG.info("Starting GUI thread...");
    try {
      final AppRootState appRootState = new AppRootState();
      final AppConfigPersistor appConfigPersistor = new AppConfigPersistor(appRootState);
      appConfigPersistor.listenChanges();
      appConfigPersistor.readConfigFile();
      final MainWindow mainWindow = new MainWindow(appRootState);
      mainWindow.init();
      mainWindow.setVisible(true);
    } catch (FatalException ex) {
      LOG.error(ex.getMessage());
      JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }
}
