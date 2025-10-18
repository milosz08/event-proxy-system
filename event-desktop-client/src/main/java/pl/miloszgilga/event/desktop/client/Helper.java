package pl.miloszgilga.event.desktop.client;

import java.util.function.Consumer;

public class Helper {
  public static <T> void ifNotNull(T object, Consumer<T> action) {
    if (object != null) {
      action.accept(object);
    }
  }
}
