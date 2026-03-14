import { AxiosInstance, AxiosResponse } from 'axios';
import {
  ApiResult,
  EventDetails,
  EventPayload,
  EventTable,
  PageableApiResults,
  SseEventPayload,
  UnreadEventsCount,
} from '../../@types/shared';
import { createScopedLogger } from '../logger';
import type { NetworkSessionManager } from '../network/network-session-manager';
import { safeEncryptedRequest, safeRequest, safeStreamRequest } from '../network/request';
import store from '../store';
import type { ConfigService } from './config-service';
import type { CryptoService, EncryptedRestMessage, EncryptedStreamMessage } from './crypto-service';

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

  private readonly MAX_RETRIES = 5;
  private readonly RETRY_INTERVAL_MS = 2000;

  private reconnectTimers = new Map<string, NodeJS.Timeout>();
  private intentionalDisconnects = new Set<string>();
  private lastSeenEventIds = new Map<string, number>();
  private reconnectAttempts = new Map<string, number>();

  constructor(
    private configService: ConfigService,
    private networkManager: NetworkSessionManager,
    private cryptoService: CryptoService,
    private handlers: Handlers
  ) {}

  public async startEventsStream(serverId: string, lastEventId?: number): Promise<boolean> {
    this.intentionalDisconnects.delete(serverId);
    this.reconnectAttempts.set(serverId, 0);
    if (lastEventId) {
      this.lastSeenEventIds.set(serverId, lastEventId);
    }
    return this.connectStreamWithRetry(serverId);
  }

  public stopEventsStream(serverId: string): void {
    this.intentionalDisconnects.add(serverId);
    this.reconnectAttempts.set(serverId, 0);
    const timer = this.reconnectTimers.get(serverId);
    if (timer) {
      clearTimeout(timer);
      this.reconnectTimers.delete(serverId);
    }
    const disconnect = this.activeStreams.get(serverId);
    if (disconnect) {
      disconnect();
      this.activeStreams.delete(serverId);
    }
  }

  public async getPageableEvents(
    serverId: string,
    eventTable: EventTable,
    subjectSearch: string,
    isAscending: boolean,
    offset: number,
    limit: number,
    eventSource: string | null = null
  ): Promise<ApiResult<PageableApiResults<EventPayload>>> {
    return this.fetchEncrypted<PageableApiResults<EventPayload>>(serverId, '/api/all/event', {
      eventTable,
      ...(eventSource ? { eventSource } : {}),
      subjectSearch,
      isAscending,
      offset,
      limit,
    });
  }

  public async getEventDetails(
    serverId: string,
    eventTable: EventTable,
    eventId: number
  ): Promise<ApiResult<EventDetails>> {
    return this.fetchEncrypted<EventDetails>(serverId, '/api/single/event', {
      eventTable,
      id: eventId,
    });
  }

  public async getCountOfUnreadEvents(
    serverId: string,
    eventTable: EventTable,
    eventSource: string | null = null
  ): Promise<ApiResult<UnreadEventsCount>> {
    return this.fetchEncrypted<UnreadEventsCount>(serverId, '/api/all/event/unread/count', {
      eventTable,
      ...(eventSource ? { eventSource } : {}),
    });
  }

  public async getEventSources(
    serverId: string,
    eventTable: EventTable
  ): Promise<ApiResult<string[]>> {
    return this.fetchEncrypted<string[]>(serverId, '/api/all/event/source', { eventTable });
  }

  public async markEventAsRead(
    serverId: string,
    eventTable: EventTable,
    eventId: number
  ): Promise<ApiResult<void>> {
    return this.performAction(
      serverId,
      '/api/single/event/read',
      `successfully perform event action with id: ${eventId} as read (${eventTable})`,
      (client, url) => client.post<void>(url, null, { params: { eventTable, id: eventId } })
    );
  }

  public async markEventAsUnread(
    serverId: string,
    eventTable: EventTable,
    eventId: number
  ): Promise<ApiResult<void>> {
    return this.performAction(
      serverId,
      '/api/single/event/unread',
      `successfully perform event action with id: ${eventId} as unread (${eventTable})`,
      (client, url) => client.post<void>(url, null, { params: { eventTable, id: eventId } })
    );
  }

  public async bulkArchiveEvents(serverId: string, eventIds: number[]): Promise<ApiResult<void>> {
    return this.performAction(
      serverId,
      '/api/bulk/event/archive',
      `successfully perform events action with id: ${eventIds} as archive`,
      (client, url) =>
        client.post<void>(url, null, {
          params: { id: eventIds },
          paramsSerializer: { indexes: null },
        })
    );
  }

  public async bulkUnarchiveEvents(serverId: string, eventIds: number[]): Promise<ApiResult<void>> {
    return this.performAction(
      serverId,
      '/api/bulk/event/unarchive',
      `successfully perform events action with id: ${eventIds} as unarchive`,
      (client, url) =>
        client.post<void>(url, null, {
          params: { id: eventIds },
          paramsSerializer: { indexes: null },
        })
    );
  }

  public async bulkDeleteEvents(
    serverId: string,
    eventTable: EventTable,
    eventIds: number[]
  ): Promise<ApiResult<void>> {
    return this.performAction(
      serverId,
      '/api/bulk/event',
      `successfully deleted events with id: ${eventIds} (${eventTable})`,
      (client, url) =>
        client.delete<void>(url, {
          params: { id: eventIds, eventTable },
          paramsSerializer: { indexes: null },
        })
    );
  }

  public async allArchiveEvents(
    serverId: string,
    eventSource: string | null = null
  ): Promise<ApiResult<void>> {
    return this.performAction(
      serverId,
      '/api/all/event/archive',
      `successfully perform all events action as archive (${eventSource})`,
      (client, url) =>
        client.post<void>(url, null, {
          params: { ...(eventSource ? { eventSource } : {}) },
          paramsSerializer: { indexes: null },
        })
    );
  }

  public async allUnarchiveEvents(
    serverId: string,
    eventSource: string | null = null
  ): Promise<ApiResult<void>> {
    return this.performAction(
      serverId,
      '/api/all/event/unarchive',
      `successfully perform all events action as unarchive (${eventSource})`,
      (client, url) =>
        client.post<void>(url, null, {
          params: { ...(eventSource ? { eventSource } : {}) },
          paramsSerializer: { indexes: null },
        })
    );
  }

  public async allDeleteEvents(
    serverId: string,
    eventTable: EventTable,
    eventSource: string | null = null
  ): Promise<ApiResult<void>> {
    return this.performAction(
      serverId,
      '/api/all/event',
      `successfully perform all events action as delete (${eventTable})`,
      (client, url) =>
        client.delete<void>(url, {
          params: { eventTable, ...(eventSource ? { eventSource } : {}) },
          paramsSerializer: { indexes: null },
        })
    );
  }

  private async connectStreamWithRetry(serverId: string): Promise<boolean> {
    if (this.intentionalDisconnects.has(serverId)) {
      return false;
    }
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return false;
    }
    const client = this.networkManager.getAxiosForServer(server);
    const url = '/stream/handshake';
    const { success, data: handshakeData } = await safeRequest<SseHandshakeRes>(
      () => client.post(url),
      server.name,
      url
    );
    if (!success || !handshakeData) {
      return this.scheduleReconnect(serverId, 'Handshake failed');
    }
    const { sessionId, aes } = handshakeData;
    const { privateKey } = store.get('rsaKeys');
    const { data: sessionKey } = this.cryptoService.decryptSessionKey(server, aes, privateKey);
    if (!sessionKey) {
      return this.scheduleReconnect(serverId, 'Session key decryption failed');
    }
    this.reconnectAttempts.set(serverId, 0);
    this.logger.info(server.name, `sse handshake performed, session id: ${sessionId}`);

    const currentLastId = this.lastSeenEventIds.get(serverId);
    let streamUrl = `/stream/events?sessionId=${sessionId}`;
    if (currentLastId) {
      streamUrl += `&lastEventId=${currentLastId}`;
    }
    this.cleanupActiveStream(serverId);
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
          if (data && data.id) {
            this.lastSeenEventIds.set(serverId, data.id);
          }
          this.handlers.onEvent(serverId, data, error);
        },
        onError: err => {
          this.cleanupActiveStream(serverId);
          this.scheduleReconnect(serverId, err?.toString() || 'Stream error');
        },
        onClose: () => {
          this.cleanupActiveStream(serverId);
          this.scheduleReconnect(serverId, 'Stream closed unexpectedly');
        },
      },
      server.name,
      streamUrl
    );
    this.activeStreams.set(serverId, disconnectFn);
    return true;
  }

  private scheduleReconnect(serverId: string, errorMsg: string): boolean {
    if (this.intentionalDisconnects.has(serverId)) {
      return false;
    }
    const currentAttempt = this.reconnectAttempts.get(serverId) || 0;
    if (currentAttempt >= this.MAX_RETRIES) {
      this.logger.error(
        serverId,
        `sse stream failed after ${this.MAX_RETRIES} attempts, giving up`,
        errorMsg
      );
      this.handlers.onEvent(serverId, undefined, errorMsg);
      return false;
    }
    const nextAttempt = currentAttempt + 1;
    this.reconnectAttempts.set(serverId, nextAttempt);
    const delayMs = nextAttempt * this.RETRY_INTERVAL_MS;
    this.logger.warn(
      serverId,
      `sse disconnected, silent reconnect in ${delayMs} ms` +
        `(attempt ${nextAttempt}/${this.MAX_RETRIES}), reason: ${errorMsg}`
    );
    const timer = setTimeout(async () => {
      await this.connectStreamWithRetry(serverId);
    }, delayMs);
    timer.unref();
    this.reconnectTimers.set(serverId, timer);
    return true;
  }

  private cleanupActiveStream(serverId: string): void {
    const disconnect = this.activeStreams.get(serverId);
    if (disconnect) {
      disconnect();
      this.activeStreams.delete(serverId);
    }
  }

  private async fetchEncrypted<T>(
    serverId: string,
    url: string,
    params: Record<string, unknown>
  ): Promise<ApiResult<T>> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return { success: false };
    }
    const client = this.networkManager.getAxiosForServer(server);
    return await safeEncryptedRequest<T>(
      () => client.get<EncryptedRestMessage>(url, { params }),
      this.cryptoService,
      server,
      url
    );
  }

  private async performAction(
    serverId: string,
    url: string,
    logMessage: string,
    requestFn: (client: AxiosInstance, url: string) => Promise<AxiosResponse<void>>
  ): Promise<ApiResult<void>> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return { success: false };
    }
    const client = this.networkManager.getAxiosForServer(server);
    const result = await safeRequest<void>(() => requestFn(client, url), server.name, url);
    if (result.success) {
      this.logger.info(server.name, logMessage);
    }
    return result;
  }
}
