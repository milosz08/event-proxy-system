module pl.miloszgilga.event.desktop.client {
  requires io.reactivex.rxjava3;
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.media;
  requires javafx.swing;
  requires org.controlsfx.controls;
  requires org.slf4j;

  opens pl.miloszgilga.event.desktop.client to javafx.fxml;
  opens pl.miloszgilga.event.desktop.client.controller to javafx.fxml;

  exports pl.miloszgilga.event.desktop.client;
  exports pl.miloszgilga.event.desktop.client.controller;
  exports pl.miloszgilga.event.desktop.client.store;
}
