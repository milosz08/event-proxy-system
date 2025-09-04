package pl.miloszgilga.event.proxy.server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SseStandaloneReceiver {
  private static final String URL = "http://localhost:4365/events";

  public static void main(String[] args) throws InterruptedException {
    final HttpClient client = HttpClient.newHttpClient();
    final HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(URL))
      .header("Accept", "text/event-stream")
      .build();

    final CompletableFuture<Void> future = client.sendAsync(
      request,
      HttpResponse.BodyHandlers.ofLines()
    ).thenAccept(response -> {
      System.out.println("connected, status: " + response.statusCode());
      response.body().forEach(System.out::println);
    }).exceptionally(e -> {
      System.err.println("unable to connect: " + e.getMessage());
      return null;
    });

    TimeUnit.MINUTES.sleep(5);
    future.cancel(true);
  }
}
