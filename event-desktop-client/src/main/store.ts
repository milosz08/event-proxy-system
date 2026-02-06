import crypto from 'crypto';
import Store from 'electron-store';
import { ServerInput } from '../@types/shared';

export type ServerConfig = {
  id: string;
  encryptedPassword: string;
  sessionCookie?: string; // as JSON string
  hasDefaultPassword?: boolean;
  unreadNotifications: number;
} & Omit<ServerInput, 'password'>;

type RsaKeys = {
  publicKey: string;
  privateKey: string;
};

type AppSchema = {
  clientId: string;
  rsaKeys: RsaKeys;
  servers: ServerConfig[];
};

const store = new Store<AppSchema>({
  defaults: {
    clientId: crypto.randomUUID(),
    rsaKeys: {
      publicKey: '',
      privateKey: '',
    },
    servers: [],
  },
});

export const ensureCryptoKeys = (): RsaKeys => {
  if (!store.get('rsaKeys.publicKey')) {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('rsa', {
      modulusLength: 2048,
      publicKeyEncoding: { type: 'spki', format: 'pem' },
      privateKeyEncoding: { type: 'pkcs8', format: 'pem' },
    });
    store.set('rsaKeys', { publicKey, privateKey });
  }
  return store.get('rsaKeys');
};

export default store;
