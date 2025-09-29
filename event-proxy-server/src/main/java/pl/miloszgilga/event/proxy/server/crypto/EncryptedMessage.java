package pl.miloszgilga.event.proxy.server.crypto;

public record EncryptedMessage(String iv, String aes, String jsonContent) {
}
