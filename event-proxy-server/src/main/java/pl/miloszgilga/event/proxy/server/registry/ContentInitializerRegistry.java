package pl.miloszgilga.event.proxy.server.registry;

import java.util.ArrayList;
import java.util.List;

public class ContentInitializerRegistry {
  private final List<ContentInitializer> initializers;

  public ContentInitializerRegistry() {
    initializers = new ArrayList<>();
  }

  public void register(ContentInitializer initializer) {
    initializers.add(initializer);
  }

  public void init() {
    for (final ContentInitializer initializer : initializers) {
      initializer.init();
    }
  }
}
