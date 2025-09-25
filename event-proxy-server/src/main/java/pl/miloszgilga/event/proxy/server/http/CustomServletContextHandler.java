package pl.miloszgilga.event.proxy.server.http;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;

import java.util.EnumSet;
import java.util.List;

class CustomServletContextHandler extends ServletContextHandler {
  private static final EnumSet<DispatcherType> D_TYPES = EnumSet.of(DispatcherType.REQUEST);

  void addFilter(Filter filter, List<String> paths) {
    for (final String path : paths) {
      super.addFilter(filter, path, D_TYPES);
    }
  }

  void addFilter(Filter filter, String path) {
    addFilter(filter, List.of(path));
  }
}
