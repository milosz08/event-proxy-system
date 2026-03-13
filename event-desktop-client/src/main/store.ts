import crypto from 'crypto';
import Store from 'electron-store';
import { ServerConfigDTO, UiConfig } from '../@types/shared';

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
};

const store = new Store<AppSchema>({
  defaults: {
    clientId: crypto.randomUUID(),
    rsaKeys: {
      publicKey: '',
      privateKey: '',
    },
    servers: [],
    uiConfig: {
      sideBySideLook: false,
      showDetails: true,
      panelSizes: [50, 50],
      sortByAscending: false,
      eventTable: 'EVENTS',
      eventSourceFilter: null,
      selectedServerId: null,
    },
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
