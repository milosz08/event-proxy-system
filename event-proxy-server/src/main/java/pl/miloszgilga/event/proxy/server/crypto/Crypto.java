package pl.miloszgilga.event.proxy.server.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class Crypto {
  private static final String ASYMMETRIC_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
  private static final String SYMMETRIC_ALGORITHM = "AES/GCM/NoPadding";
  private static final int AES_KEY_SIZE = 256;
  private static final int GCM_IV_LENGTH = 12; // 96 bits
  private static final int GCM_TAG_LENGTH = 16; // 128 bits

  public static SecretKey createAesKey() throws NoSuchAlgorithmException {
    final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(AES_KEY_SIZE);
    return keyGenerator.generateKey();
  }

  public static PublicKey reconstructPubKey(String rsaPubKeyBase64) throws Exception {
    // IMPORTANT! sanitize rsa public key
    final String rsaPubKeySan = rsaPubKeyBase64.replaceAll("[^A-Za-z0-9+/=]", "");
    final byte[] publicKeyBytes = Base64.getDecoder().decode(rsaPubKeySan);
    final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
    final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePublic(keySpec);
  }

  public static String encryptAesKey(SecretKey aesKey, PublicKey pubKey) throws Exception {
    final Cipher rsaCipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM);
    rsaCipher.init(Cipher.ENCRYPT_MODE, pubKey);
    final byte[] encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.getEncoded());
    return Base64.getEncoder().encodeToString(encryptedAesKeyBytes);
  }

  public static AesEncryptedBase64Data encryptDataAes(String plainData, SecretKey aesKey)
    throws Exception {
    // encrypt data using AES key
    final Cipher aesCipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
    final SecureRandom secureRandom = new SecureRandom();

    // generate initiate vector (iv)
    final byte[] iv = new byte[GCM_IV_LENGTH];
    secureRandom.nextBytes(iv);

    final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
    aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParameterSpec);

    final byte[] encryptedDataWithTag = aesCipher
      .doFinal(plainData.getBytes(StandardCharsets.UTF_8));

    final int ciphertextLength = encryptedDataWithTag.length - GCM_TAG_LENGTH;
    final byte[] ciphertext = Arrays.copyOfRange(encryptedDataWithTag, 0, ciphertextLength);
    final byte[] tag = Arrays.copyOfRange(encryptedDataWithTag, ciphertextLength,
      encryptedDataWithTag.length);

    return new AesEncryptedBase64Data(
      Base64.getEncoder().encodeToString(ciphertext),
      Base64.getEncoder().encodeToString(iv),
      Base64.getEncoder().encodeToString(tag)
    );
  }
}
