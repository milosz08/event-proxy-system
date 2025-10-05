package pl.miloszgilga.event.desktop.client.state;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public abstract class AbstractDisposableProvider {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractDisposableProvider.class);

  private final ConcurrentMap<String, Disposable> subscriptionsPool;

  protected AbstractDisposableProvider() {
    subscriptionsPool = new ConcurrentHashMap<>();
  }

  public <T> void wrapAsDisposable(Observable<T> subject, Consumer<T> consumer) {
    final Disposable disposable = subject.subscribe(consumer::accept);
    subscriptionsPool.put(UUID.randomUUID().toString(), disposable);
  }

  public void disposeAllSubscriptions() {
    for (final Map.Entry<String, Disposable> entry : subscriptionsPool.entrySet()) {
      entry.getValue().dispose();
    }
    LOG.info("Disposed all subscriptions: {}", subscriptionsPool.size());
    afterDispose();
  }

  protected abstract void afterDispose();
}
