package pl.miloszgilga.event.proxy.server;

import java.util.ArrayList;
import java.util.List;

class ContentInitializerRegistry {
  private final List<ContentInitializer> initializers;

  ContentInitializerRegistry() {
    initializers = new ArrayList<>();
  }

  void register(ContentInitializer initializer) {
    initializers.add(initializer);
  }

  void init() {
    for (final ContentInitializer initializer : initializers) {
      initializer.init();
    }
  }
}
