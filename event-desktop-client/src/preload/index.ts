import { IpcRendererEvent, contextBridge, ipcRenderer } from 'electron';
import {
  ApiResult,
  EventDetails,
  EventPayload,
  EventTable,
  LoginData,
  PageableApiResults,
  ResponseResult,
  ServerConfigDTO,
  ServerInput,
  SseEventPayload,
  UiConfig,
} from '../@types/shared';

const api = {
  // badge
  onBadgeSyncAll: (callback: (counts: Record<string, number>) => void) => {
    const listener = (_: IpcRendererEvent, counts: Record<string, number>): void =>
      callback(counts);
    ipcRenderer.on('badge:sync-all', (_, counts) => callback(counts));
    return () => {
      ipcRenderer.removeListener('badge:sync-all', listener);
    };
  },
  // servers
  addServer: (data: ServerInput): Promise<string> => {
    return ipcRenderer.invoke('server:add', data);
  },
  getServers: (): Promise<ServerConfigDTO[]> => {
    return ipcRenderer.invoke('server:get-all');
  },
  removeServer: (serverId: string): Promise<ResponseResult> => {
    return ipcRenderer.invoke('server:remove', serverId);
  },
  onHeartbeat: (callback: (serverId: string, status: boolean, resTimeMillis?: number) => void) => {
    const listener = (
      _: IpcRendererEvent,
      serverId: string,
      status: boolean,
      resTimeMillis?: number
    ): void => callback(serverId, status, resTimeMillis);
    ipcRenderer.on('server:heartbeat', listener);
    return () => {
      ipcRenderer.removeListener('server:heartbeat', listener);
    };
  },
  // sse
  onSseEvent: (
    callback: (
      serverId: string,
      payload: SseEventPayload | undefined,
      error: string | undefined
    ) => void
  ) => {
    const listener = (
      _: IpcRendererEvent,
      serverId: string,
      payload: SseEventPayload | undefined,
      error: string | undefined
    ): void => callback(serverId, payload, error);
    ipcRenderer.on('sse:event', listener);
    return () => {
      ipcRenderer.removeListener('sse:event', listener);
    };
  },
  // auth
  connect: (serverId: string): Promise<ApiResult<LoginData>> => {
    return ipcRenderer.invoke('auth:connect', serverId);
  },
  disconnect: (serverId: string): Promise<boolean> => {
    return ipcRenderer.invoke('auth:disconnect', serverId);
  },
  updateDefaultPassword: (serverId: string, newPassword: string): Promise<ResponseResult> => {
    return ipcRenderer.invoke('auth:update-password', { serverId, newPassword });
  },
  getInitialSessions: (): Promise<string[]> => {
    return ipcRenderer.invoke('auth:get-initial-sessions');
  },
  onSessionExpired: (callback: (serverId: string) => void) => {
    const listener = (_: IpcRendererEvent, serverId: string): void => callback(serverId);
    ipcRenderer.on('auth:session-expired', listener);
    return () => {
      ipcRenderer.removeListener('auth:session-expired', listener);
    };
  },
  // ui config
  getUiConfig: (): Promise<UiConfig> => {
    return ipcRenderer.invoke('ui-config:get');
  },
  updateUiConfig: (uiConfig: Partial<UiConfig>): Promise<UiConfig> => {
    return ipcRenderer.invoke('ui-config:set', uiConfig);
  },
  // event source
  getEventSources: (serverId: string, eventTable: EventTable): Promise<ApiResult<string[]>> => {
    return ipcRenderer.invoke('event-source:all', { serverId, eventTable });
  },
  // events
  getPageableEvents: (
    serverId: string,
    eventTable: EventTable,
    subjectSearch: string,
    isAscending: boolean,
    offset: number,
    limit: number,
    eventSource: string | null = null
  ): Promise<ApiResult<PageableApiResults<EventPayload>>> => {
    return ipcRenderer.invoke('events:pageable-all', {
      serverId,
      eventTable,
      subjectSearch,
      isAscending,
      offset,
      limit,
      eventSource,
    });
  },
  getEventDetails: (
    serverId: string,
    eventTable: EventTable,
    eventId: number
  ): Promise<ApiResult<EventDetails>> => {
    return ipcRenderer.invoke('events:details-single', {
      serverId,
      eventTable,
      eventId,
    });
  },
  markEventAsRead: (
    serverId: string,
    eventTable: EventTable,
    eventId: number
  ): Promise<ApiResult<void>> => {
    return ipcRenderer.invoke('events:mark-as-read-single', { serverId, eventTable, eventId });
  },
  markEventAsUnread: (
    serverId: string,
    eventTable: EventTable,
    eventId: number
  ): Promise<ApiResult<void>> => {
    return ipcRenderer.invoke('events:mark-as-unread-single', { serverId, eventTable, eventId });
  },
  bulkArchiveEvents: (serverId: string, eventIds: number[]): Promise<ApiResult<void>> => {
    return ipcRenderer.invoke('events:archive-bulk', { serverId, eventIds });
  },
  bulkUnarchiveEvents: (serverId: string, eventIds: number[]): Promise<ApiResult<void>> => {
    return ipcRenderer.invoke('events:unarchive-bulk', { serverId, eventIds });
  },
  bulkDeleteEvents: (
    serverId: string,
    eventTable: EventTable,
    eventIds: number[]
  ): Promise<ApiResult<void>> => {
    return ipcRenderer.invoke('events:delete-bulk', { serverId, eventTable, eventIds });
  },
  allArchiveEvents: (
    serverId: string,
    eventSource: string | null = null
  ): Promise<ApiResult<void>> => {
    return ipcRenderer.invoke('events:archive-all', { serverId, eventSource });
  },
  allUnarchiveEvents: (
    serverId: string,
    eventSource: string | null = null
  ): Promise<ApiResult<void>> => {
    return ipcRenderer.invoke('events:unarchive-all', { serverId, eventSource });
  },
  allDeleteEvents: (
    serverId: string,
    eventTable: EventTable,
    eventSource: string | null = null
  ): Promise<ApiResult<void>> => {
    return ipcRenderer.invoke('events:delete-all', { serverId, eventTable, eventSource });
  },
};

contextBridge.exposeInMainWorld('api', api);
