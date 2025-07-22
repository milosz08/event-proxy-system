package pl.miloszgilga.event.proxy.server;

class EventProxyServerMain implements Runnable {
  private final HttpProxyServerThread httpProxyServerThread;
  private final SmtpProxyServerThread smtpProxyServerThread;

  EventProxyServerMain() {
    this.httpProxyServerThread = new HttpProxyServerThread(4365);
    this.smtpProxyServerThread = new SmtpProxyServerThread(1025, 10);
  }

  public static void main(String[] args) {
    final EventProxyServerMain main = new EventProxyServerMain();
    Runtime.getRuntime().addShutdownHook(new Thread(main));
    main.start();
  }

  void start() {
    httpProxyServerThread.start();
    smtpProxyServerThread.start();
  }

  @Override
  public void run() {
    httpProxyServerThread.stop();
    smtpProxyServerThread.stop();
  }
}
