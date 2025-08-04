package pl.miloszgilga.event.proxy.server;

import java.util.List;

interface EmailPersistor extends ContentInitializer {
  // executed with blocking mode
  void persist(String dataName, List<EmailPropertyValue> emailData);
}
