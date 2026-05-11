import crypto from 'crypto';
import { app } from 'electron';
import Store from 'electron-store';
import { ServerConfigDTO, UiConfig } from '../@types/shared';
import { defaultUiConfig } from '../data/shared';

export type ServerConfig = {
  encryptedPassword: string;
  sessionCookie?: string; // as JSON string
} & ServerConfigDTO;

type RsaKeys = {
  publicKey: string;
  privateKey: string;
};

type AppSchema = {
  clientId: string;
  rsaKeys: RsaKeys;
  servers: ServerConfig[];
  uiConfig: UiConfig;
  closeAppCompletely: boolean;
  openAtLogin: boolean;
};

if (!app.isPackaged) {
  const defaultUserDataPath = app.getPath('userData');
  app.setPath('userData', `${defaultUserDataPath}-dev`);
  app.setName(`${app.getName()}-dev`);
}

const store = new Store<AppSchema>({
  defaults: {
    clientId: crypto.randomUUID(),
    rsaKeys: {
      publicKey: '',
      privateKey: '',
    },
    servers: [],
    uiConfig: defaultUiConfig,
    closeAppCompletely: false,
    openAtLogin: false,
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
