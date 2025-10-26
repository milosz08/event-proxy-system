package pl.miloszgilga.event.proxy.server.crypto;

public record AesEncryptedBase64Data(String cipher, String iv, String tag) {
}
