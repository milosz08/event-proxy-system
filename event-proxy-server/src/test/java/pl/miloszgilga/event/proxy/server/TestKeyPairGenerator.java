package pl.miloszgilga.event.proxy.server;

import java.security.*;
import java.util.Base64;

public class TestKeyPairGenerator {
  public static void main(String[] args) throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);

    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivate();
    PublicKey publicKey = keyPair.getPublic();

    final String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
    final String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());

    System.out.println("private:");
    System.out.println(privateKeyBase64);

    System.out.println("public:");
    System.out.println(publicKeyBase64);
  }
}
