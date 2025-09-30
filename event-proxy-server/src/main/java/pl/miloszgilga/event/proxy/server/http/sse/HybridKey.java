package pl.miloszgilga.event.proxy.server.http.sse;

import javax.crypto.SecretKey;

record HybridKey(SecretKey aes, String encryptedAes) {
}
