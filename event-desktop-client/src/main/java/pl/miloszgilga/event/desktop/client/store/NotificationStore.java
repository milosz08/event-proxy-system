package pl.miloszgilga.event.desktop.client.store;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class NotificationStore {
  private final PublishSubject<Action> actionSubject = PublishSubject.create();
  private final BehaviorSubject<Integer> countStore = BehaviorSubject.createDefault(0);

  public NotificationStore() {
    actionSubject.scan(0, (currentCount, action) -> switch (action) {
      case INCREASE -> currentCount + 1;
      case DECREASE -> Math.max(0, currentCount - 1);
      case CLEAR -> 0;
    }).subscribe(countStore);
  }

  public void increase() {
    actionSubject.onNext(Action.INCREASE);
  }

  public void decrease() {
    actionSubject.onNext(Action.DECREASE);
  }

  public void clear() {
    actionSubject.onNext(Action.CLEAR);
  }

  public Observable<Integer> getCountStream() {
    return countStore.distinctUntilChanged().hide();
  }

  private enum Action {
    INCREASE,
    DECREASE,
    CLEAR,
  }
}
