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

    const url = '/stream/handshake';
    const { success, data } = await safeRequest<SseHandshakeRes>(
      () => client.post(url),
      server.name,
      url
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
        onClose: () => this.handlers.onEvent(serverId, undefined, undefined),
      },
      server.name,
      streamUrl
    );
    this.activeStreams.set(serverId, disconnectFn);
    return true;
  }

  public stopEventsStream(serverId: string): void {
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
