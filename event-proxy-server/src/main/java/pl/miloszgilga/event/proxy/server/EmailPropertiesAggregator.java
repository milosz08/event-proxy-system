package pl.miloszgilga.event.proxy.server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

record EmailPropertiesAggregator(List<EmailPropertyValue> propertyValues) {
  String serializeToJson(String eventSource) {
    final JSONObject root = new JSONObject();
    final JSONArray dataFields = new JSONArray();
    for (final EmailPropertyValue eventProperty : propertyValues) {
      final JSONObject property = new JSONObject();
      property.put("name", eventProperty.name());
      property.put("value", eventProperty.value());
      property.put("type", eventProperty.fieldType().name());
      dataFields.put(property);
    }
    root.put("eventSource", eventSource);
    root.put("dataFields", dataFields);
    return root.toString();
  }
}
