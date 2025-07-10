package pl.miloszgilga.event.proxy.server;

class EventProxyServerMain {
  public static void main(String[] args) {
    final HttpProxyServerThread httpProxyServerThread = new HttpProxyServerThread(4365);
    final SmtpProxyServerThread smtpProxyServerThread = new SmtpProxyServerThread(1025, 10);

    httpProxyServerThread.start();
    smtpProxyServerThread.start();
  }
}
