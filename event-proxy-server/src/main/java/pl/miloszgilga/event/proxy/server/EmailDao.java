package pl.miloszgilga.event.proxy.server;

interface EmailDao extends ContentInitializer {
  // executed with blocking mode
  void persist(String dataName, EmailPropertiesAggregator emailData);
}
