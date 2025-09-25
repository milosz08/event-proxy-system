package pl.miloszgilga.event.proxy.server;

public abstract class AbstractThread implements Runnable {
  private final String name;

  private Thread thread;
  protected boolean running = false;

  public AbstractThread(String name) {
    this.name = name;
  }

  public final void start() {
    running = true;
    thread = new Thread(this);
    thread.setName(name);
    thread.start();
  }

  public final void stop() {
    running = false;
    beforeStopThread();
    thread.interrupt();
    thread = null;
  }

  protected abstract void beforeStopThread();
}
