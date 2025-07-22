package pl.miloszgilga.event.proxy.server;

abstract class AbstractThread implements Runnable {
  private final String name;

  private Thread thread;
  protected boolean running = false;

  AbstractThread(String name) {
    this.name = name;
  }

  final void start() {
    running = true;
    thread = new Thread(this);
    thread.setName(name);
    thread.start();
  }

  final void stop() {
    running = false;
    beforeStopThread();
    thread.interrupt();
    thread = null;
  }

  abstract void beforeStopThread();
}
