package pl.miloszgilga.event.proxy.server;

interface EmailPersistor extends ContentInitializer {
  // executed with blocking mode
  void persist(String dataName, EmailPropertiesAggregator emailData);
}
