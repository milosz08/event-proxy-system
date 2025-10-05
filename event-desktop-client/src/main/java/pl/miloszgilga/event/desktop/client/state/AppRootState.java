package pl.miloszgilga.event.desktop.client.state;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppRootState extends AbstractDisposableProvider {
  private final BehaviorSubject<List<ProxyServerData>> persistedProxyServers$;
  private final BehaviorSubject<Optional<ProxyServerData>> connectedProxyServerData$;

  public AppRootState() {
    persistedProxyServers$ = BehaviorSubject.createDefault(new ArrayList<>());
    connectedProxyServerData$ = BehaviorSubject.createDefault(Optional.empty());
  }

  public void updatePersistedProxyServers(List<ProxyServerData> persistedProxyServers) {
    persistedProxyServers$.onNext(persistedProxyServers);
  }

  public void updateConnectedProxyServerData(ProxyServerData data) {
    connectedProxyServerData$.onNext(data == null ? Optional.empty() : Optional.of(data));
  }

  public Observable<List<ProxyServerData>> getPersistedProxyServers$() {
    return persistedProxyServers$.hide();
  }

  public Observable<Optional<ProxyServerData>> getConnectedProxyServerData$() {
    return connectedProxyServerData$.hide();
  }

  @Override
  protected void afterDispose() {
  }
}
