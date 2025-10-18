package pl.miloszgilga.event.desktop.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.desktop.client.controller.RootController;
import pl.miloszgilga.event.desktop.client.store.NotificationStore;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class EventDesktopClientMain extends Application {
  public static final String APP_NAME = "Notifications client";
  private static final Logger LOG = LoggerFactory.getLogger(EventDesktopClientMain.class);
  private static final int WIDTH = 1280;
  private static final int HEIGHT = 720;

  private final NotificationStore notificationStore = new NotificationStore();

  private BadgeManager badgeManager;
  private RootController rootController;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws IOException {
    badgeManager = new BadgeManager(stage, notificationStore);

    final URL url = EventDesktopClientMain.class.getResource("views/root-view.fxml");
    final FXMLLoader fxmlLoader = new FXMLLoader(url);
    final Parent root = fxmlLoader.load();

    rootController = fxmlLoader.getController();
    rootController.setNotificationStore(notificationStore);

    final Scene scene = new Scene(root, WIDTH, HEIGHT);
    stage.setTitle(APP_NAME);
    stage.setScene(scene);
    stage.setOnCloseRequest(this::onCloseHandler);
    stage.show();
  }

  @Override
  public void stop() {
    LOG.info("Gracefully shutting down and clearing all subscriptions...");
    rootController.close();
    badgeManager.close();
  }

  private void onCloseHandler(WindowEvent event) {
    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

    alert.setTitle("Confirm Exit");
    alert.setHeaderText("Are you sure you want to exit the application?");
    alert.setContentText("New events will no longer be received if you close the client.");
    alert.initStyle(StageStyle.UTILITY);

    final Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      Platform.exit();
    } else {
      event.consume();
    }
  }
}
