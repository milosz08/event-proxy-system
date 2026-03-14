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

declare global {
  interface Window {
    api: {
      // badge
      getAllBadgeCounts: () => Promise<Record<string, number>>;
      onBadgeSyncAll: (callback: (counts: Record<string, number>) => void) => () => void;
      // servers
      addServer: (data: ServerInput) => Promise<string>;
      getServers: () => Promise<ServerConfigDTO[]>;
      removeServer: (serverId: string) => Promise<ResponseResult>;
      onHeartbeat: (
        callback: (serverId: string, status: boolean, resTimeMillis?: number) => void
      ) => () => void;
      // sse
      onSseEvent: (
        callback: (
          serverId: string,
          payload: SseEventPayload | undefined,
          error: string | undefined
        ) => void
      ) => () => void;
      // auth
      connect: (serverId: string) => Promise<ApiResult<LoginData>>;
      disconnect: (serverId: string) => Promise<boolean>;
      updateDefaultPassword: (serverId: string, newPassword: string) => Promise<ResponseResult>;
      getInitialSessions: () => Promise<string[]>;
      onSessionExpired: (callback: (serverId: string) => void) => () => void;
      // ui config
      getUiConfig: () => Promise<UiConfig>;
      updateUiConfig: (uiConfig: Partial<UiConfig>) => Promise<UiConfig>;
      // event source
      getEventSources: (serverId: string, eventTable: EventTable) => Promise<ApiResult<string[]>>;
      // events
      getPageableEvents: (
        serverId: string,
        eventTable: EventTable,
        subjectSearch: string,
        isAscending: boolean,
        offset: number,
        limit: number,
        eventSource: string | null
      ) => Promise<ApiResult<PageableApiResults<EventPayload>>>;
      getEventDetails: (
        serverId: string,
        eventTable: EventTable,
        eventId: number
      ) => Promise<ApiResult<EventDetails>>;
      markEventAsRead: (
        serverId: string,
        eventTable: EventTable,
        eventId: number
      ) => Promise<ApiResult<void>>;
      markEventAsUnread: (
        serverId: string,
        eventTable: EventTable,
        eventId: number
      ) => Promise<ApiResult<void>>;
      bulkArchiveEvents: (serverId: string, eventIds: number[]) => Promise<ApiResult<void>>;
      bulkUnarchiveEvents: (serverId: string, eventIds: number[]) => Promise<ApiResult<void>>;
      bulkDeleteEvents: (
        serverId: string,
        eventTable: EventTable,
        eventIds: number[]
      ) => Promise<ApiResult<void>>;
      allArchiveEvents: (serverId: string, eventSource: string | null) => Promise<ApiResult<void>>;
      allUnarchiveEvents: (
        serverId: string,
        eventSource: string | null
      ) => Promise<ApiResult<void>>;
      allDeleteEvents: (
        serverId: string,
        eventTable: EventTable,
        eventSource: string | null
      ) => Promise<ApiResult<void>>;
    };
  }
}

export {};
