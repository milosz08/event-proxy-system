import { constants, createDecipheriv, privateDecrypt } from 'crypto';
import { createScopedLogger } from '../logger';
import type { ServerConfig } from '../store';
import { extractErrorMessage } from '../utils';

export type EncryptedStreamMessage = {
  cipher: string;
  iv: string;
  tag: string;
};

export type EncryptedRestMessage = {
  aes: string;
} & EncryptedStreamMessage;

type Result<T> = {
  data?: T;
  error?: string;
};

export class CryptoService {
  private logger = createScopedLogger(this.constructor.name);

  public decryptSessionKey(
    server: ServerConfig,
    encryptedAesKeyBase64: string,
    privateKeyPem: string
  ): Result<Buffer> {
    try {
      const buffer = Buffer.from(encryptedAesKeyBase64, 'base64');
      const sessionKey = privateDecrypt(
        {
          key: privateKeyPem,
          padding: constants.RSA_PKCS1_OAEP_PADDING,
          oaepHash: 'sha256',
        },
        buffer
      );
      return { data: sessionKey };
    } catch (err) {
      const errMessage = extractErrorMessage(err);
      this.logger.error(server.name, 'rsa decryption failed', errMessage);
      return { error: errMessage };
    }
  }

  public decryptPayload<T>(
    server: ServerConfig,
    message: EncryptedStreamMessage,
    sessionKey: Buffer
  ): Result<T> {
    try {
      const ivBuffer = Buffer.from(message.iv, 'base64');
      const tagBuffer = Buffer.from(message.tag, 'base64');
      const encryptedBuffer = Buffer.from(message.cipher, 'base64');

      const decipher = createDecipheriv('aes-256-gcm', sessionKey, ivBuffer);
      decipher.setAuthTag(tagBuffer);

      let decrypted = decipher.update(encryptedBuffer);
      decrypted = Buffer.concat([decrypted, decipher.final()]);

      const jsonString = decrypted.toString('utf8');
      return { data: JSON.parse(jsonString) as T };
    } catch (err) {
      const errMessage = extractErrorMessage(err);
      this.logger.error(server.name, 'aes-gcm decryption failed', errMessage);
      return { error: errMessage };
    }
  }
}
