import { SseEventPayload } from '../../@types/shared';
import { createScopedLogger } from '../logger';
import type { NetworkSessionManager } from '../network/network-session-manager';
import { safeRequest, safeStreamRequest } from '../network/request';
import store from '../store';
import type { ConfigService } from './config-service';
import type { CryptoService, EncryptedStreamMessage } from './crypto-service';

type SseHandshakeRes = {
  sessionId: string;
  aes: string;
};

type Handlers = {
  onEvent: (
    serverId: string,
    payload: SseEventPayload | undefined,
    error: string | undefined
  ) => void;
};

export class EventService {
  private logger = createScopedLogger(this.constructor.name);
  private activeStreams = new Map<string, () => void>();

  constructor(
    private configService: ConfigService,
    private networkManager: NetworkSessionManager,
    private cryptoService: CryptoService,
    private handlers: Handlers
  ) {}

  // lastEventId for prevent race conditions between REST and SSE in hybrid mode
  public async startEventsStream(serverId: string, lastEventId?: number): Promise<boolean> {
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
    this.stopEventsStream(serverId);
    const disconnectFn = await safeStreamRequest<EncryptedStreamMessage>(
      client,
      streamUrl,
      {
        onData: encryptedData => {
          const { data, error } = this.cryptoService.decryptPayload<SseEventPayload>(
            server,
            encryptedData,
            sessionKey
          );
          this.handlers.onEvent(serverId, data, error);
        },
        onError: err => this.handlers.onEvent(serverId, undefined, err),
      },
      server.name,
      'sse'
    );
    this.activeStreams.set(serverId, disconnectFn);
    return true;
  }

  public stopEventsStream(serverId: string): void {
    const disconnect = this.activeStreams.get(serverId);
    const server = this.configService.getServerById(serverId);
    if (server && disconnect) {
      disconnect();
      this.activeStreams.delete(serverId);
    }
  }
}
