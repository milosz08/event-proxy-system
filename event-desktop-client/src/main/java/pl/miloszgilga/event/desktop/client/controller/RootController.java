package pl.miloszgilga.event.desktop.client.controller;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import pl.miloszgilga.event.desktop.client.store.NotificationStore;

import java.io.Closeable;

public class RootController implements Closeable {
  private final CompositeDisposable disposables = new CompositeDisposable();

  private NotificationStore notificationStore;

  @FXML
  private Button decreaseButton;

  @FXML
  private Button clearButton;

  @FXML
  private void initialize() {
    // init state
    decreaseButton.setDisable(true);
    clearButton.setDisable(true);
  }

  public void setNotificationStore(NotificationStore notificationStore) {
    this.notificationStore = notificationStore;
    disposables.add(
      notificationStore.getCountStream().subscribe(count -> Platform.runLater(() -> {
        decreaseButton.setDisable(count <= 0);
        clearButton.setDisable(count <= 0);
      }))
    );
  }

  @FXML
  protected void onIncreaseNotification() {
    notificationStore.increase();
  }

  @FXML
  protected void onDecreaseNotification() {
    notificationStore.decrease();
  }

  @FXML
  protected void onClearNotification() {
    notificationStore.clear();
  }

  @Override
  public void close() {
    disposables.clear();
  }
}
