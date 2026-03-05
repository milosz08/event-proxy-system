import { SseEventMessage } from '../@types/shared';
import type { ConfigService } from './config-service';
import { CryptoService, EncryptedStreamMessage } from './crypto-service';
import { createScopedLogger } from './logger';
import type { NetworkSessionManager } from './network-session-manager';
import { safeRequest, safeStreamRequest } from './request';
import store from './store';

type SseHandshakeRes = {
  sessionId: string;
  aes: string;
};

type Handlers = {
  onEventMessage: (
    serverId: string,
    payload: SseEventMessage | undefined,
    error: string | undefined
  ) => void;
};

export class EventStreamService {
  private logger = createScopedLogger(this.constructor.name);
  private activeStreams = new Map<string, () => void>();

  constructor(
    private configService: ConfigService,
    private networkManager: NetworkSessionManager,
    private cryptoService: CryptoService,
    private handlers: Handlers
  ) {}

  // lastEventId for prevent race conditions between REST and SSE in hybrid mode
  public async startStream(serverId: string, lastEventId?: number): Promise<boolean> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return false;
    }
    const client = this.networkManager.getAxiosForServer(server);

    const { success, data } = await safeRequest<SseHandshakeRes>(
      () => client.post('/stream/handshake'),
      server.name,
      'handshake'
    );
    if (!success || !data) {
      return false;
    }
    const { sessionId, aes } = data;
    const { privateKey } = store.get('rsaKeys');
    const { data: sessionKey } = this.cryptoService.decryptSessionKey(server, aes, privateKey);
    if (!sessionKey) {
      return false;
    }
    this.logger.info(server.name, `sse handshake performed, session id: ${sessionId}`);

    let streamUrl = `/stream/events?sessionId=${sessionId}`;
    if (lastEventId) {
      streamUrl += `&lastEventId=${lastEventId}`;
    }
    this.stopStream(serverId);
    const disconnectFn = await safeStreamRequest<EncryptedStreamMessage>(
      client,
      streamUrl,
      {
        onData: encryptedData => {
          const { data, error } = this.cryptoService.decryptPayload<SseEventMessage>(
            server,
            encryptedData,
            sessionKey
          );
          this.handlers.onEventMessage(serverId, data, error);
        },
        onError: err => this.handlers.onEventMessage(serverId, undefined, err),
      },
      server.name,
      'sse'
    );
    this.activeStreams.set(serverId, disconnectFn);
    return true;
  }

  public stopStream(serverId: string): void {
    const disconnect = this.activeStreams.get(serverId);
    const server = this.configService.getServerById(serverId);
    if (server && disconnect) {
      disconnect();
      this.activeStreams.delete(serverId);
    }
  }
}
