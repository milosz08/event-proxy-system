import { defaultUiConfig } from '@shared-data/shared';
import { EventPayload, ServerConfigDTO, SseEventPayload, UiConfig } from '@shared-types/shared';
import { create } from 'zustand';

export type ServerConfig = Omit<ServerConfigDTO, 'hasDefaultPassword'>;

type AppState = {
  // state
  uiIsLoading: boolean;
  servers: Map<string, Omit<ServerConfig, 'id'>>;
  uiConfig: UiConfig;
  searchValue: string;
  activeSessions: Set<string>;
  updateDefaultPasswordServerId: string | null;
  serversDrawerActive: boolean;
  addServerDrawerActive: boolean;
  selectedEvents: number[];
  events: EventPayload[];
  hasMoreEvents: boolean;
  loadedHistoricalRecords: number;
  unreadNotifications: Record<string, number>;
  // actions
  setServers: (servers: ServerConfig[]) => void;
  setUiConfig: (uiConfig: UiConfig) => void;
  setSearchValue: (searchValue: string) => void;
  removeServer: (id: string) => void;
  setActiveSessions: (ids: string[]) => void;
  addActiveSession: (id: string) => void;
  removeActiveSession: (id: string) => void;
  openDefaultPasswordDialog: (serverId: string) => void;
  closeDefaultPasswordDialog: () => void;
  openServersDrawer: () => void;
  closeServersDrawer: () => void;
  openAddServerDrawer: () => void;
  closeAddServerDrawer: () => void;
  setSelectedEvents: (ids: number[]) => void;
  removeSelectedEvents: (ids: number[]) => void;
  updateHeartbeat: (serverId: string, status: boolean, resTimeMillis?: number) => void;
  setEvents: (events: EventPayload[]) => void;
  appendEvents: (newEvents: EventPayload[]) => void;
  removeEvents: (ids: number[]) => void;
  setHasMoreEvents: (hasMore: boolean) => void;
  updateEvent: (eventId: number, changes: Partial<EventPayload>) => void;
  insertLiveEvent: (newEvent: SseEventPayload) => void;
  setUnreadNotifications: (counts: Record<string, number>) => void;
  updateSseStatus: (serverId: string, isConnected: boolean) => void;
};

export const useAppStore = create<AppState>(set => ({
  // state
  uiIsLoading: true,
  servers: new Map(),
  uiConfig: defaultUiConfig,
  searchValue: '',
  activeSessions: new Set(),
  updateDefaultPasswordServerId: null,
  serversDrawerActive: false,
  addServerDrawerActive: false,
  selectedEvents: [],
  events: [],
  hasMoreEvents: true,
  loadedHistoricalRecords: 0,
  unreadNotifications: {},
  // actions
  setServers: servers =>
    set(state => ({
      ...state,
      servers: new Map(servers.map(server => [server.id, server])),
    })),
  setUiConfig: uiConfig => set(state => ({ ...state, uiConfig })),
  setSearchValue: searchValue => set(state => ({ ...state, searchValue })),
  removeServer: id =>
    set(state => {
      const newServers = new Map(state.servers);
      newServers.delete(id);
      return {
        ...state,
        servers: newServers,
        activeSessions: new Set([...state.activeSessions].filter(sessionId => sessionId !== id)),
        uiConfig: {
          ...state.uiConfig,
          selectedServerId:
            state.uiConfig.selectedServerId === id ? null : state.uiConfig.selectedServerId,
        },
      };
    }),
  setActiveSessions: ids =>
    set(state => ({
      ...state,
      uiIsLoading: false,
      activeSessions: new Set(ids),
    })),
  addActiveSession: id =>
    set(state => {
      const newSet = new Set(state.activeSessions);
      newSet.add(id);
      return { ...state, activeSessions: newSet };
    }),
  removeActiveSession: id =>
    set(state => {
      const newSet = new Set(state.activeSessions);
      newSet.delete(id);
      return {
        ...state,
        activeSessions: newSet,
      };
    }),
  openDefaultPasswordDialog: serverId => set({ updateDefaultPasswordServerId: serverId }),
  closeDefaultPasswordDialog: () => set({ updateDefaultPasswordServerId: null }),
  openServersDrawer: () => set({ serversDrawerActive: true }),
  closeServersDrawer: () => set({ serversDrawerActive: false }),
  openAddServerDrawer: () => set({ addServerDrawerActive: true }),
  closeAddServerDrawer: () => set({ addServerDrawerActive: false }),
  setSelectedEvents: ids =>
    set(state => ({
      ...state,
      selectedEvents: ids,
    })),
  removeSelectedEvents: ids =>
    set(state => ({
      ...state,
      selectedEvents: state.selectedEvents.filter(id => !ids.includes(id)),
    })),
  updateHeartbeat: (serverId, status, resTimeMillis) =>
    set(state => {
      if (!state.servers.has(serverId)) {
        return state;
      }
      const newServers = new Map(state.servers);
      const existingServer = newServers.get(serverId);
      if (!existingServer) {
        return state;
      }
      newServers.set(serverId, {
        ...existingServer,
        lastHeartbeatTimestamp: Date.now(),
        lastHeartbeatStatus: status,
        lastHeartbeatResTimeMillis: resTimeMillis,
      });
      return {
        ...state,
        servers: newServers,
      };
    }),
  setEvents: events =>
    set(state => ({
      ...state,
      events,
      loadedHistoricalRecords: events.length,
    })),
  appendEvents: newEvents =>
    set(state => ({
      ...state,
      events: [...state.events, ...newEvents],
      loadedHistoricalRecords: state.loadedHistoricalRecords + newEvents.length,
    })),
  removeEvents: ids =>
    set(state => ({
      ...state,
      events: state.events.filter(({ id }) => !ids.includes(id)),
    })),
  setHasMoreEvents: hasMoreEvents => set(state => ({ ...state, hasMoreEvents })),
  updateEvent: (eventId, changes) =>
    set(state => ({
      events: state.events.map(event => (event.id === eventId ? { ...event, ...changes } : event)),
    })),
  insertLiveEvent: (newEvent: SseEventPayload) =>
    set(state => {
      const {
        uiConfig: { eventSourceFilter, eventTable, sortByAscending },
        hasMoreEvents,
      } = state;
      if (eventTable !== 'EVENTS') {
        return state;
      }
      if (eventSourceFilter && eventSourceFilter !== newEvent.eventSource) {
        return state;
      }
      if (state.events.some(e => e.id === newEvent.id)) {
        return state;
      }
      const event = { ...newEvent, isUnread: true };
      if (sortByAscending) {
        if (hasMoreEvents) {
          return state;
        }
        return { ...state, events: [...state.events, event] };
      }
      return { ...state, events: [event, ...state.events] };
    }),
  setUnreadNotifications: counts => set(state => ({ ...state, unreadNotifications: counts })),
  updateSseStatus: (serverId, isConnected) =>
    set(state => {
      const newServers = new Map(state.servers);
      const server = newServers.get(serverId);
      if (server) {
        newServers.set(serverId, {
          ...server,
          sseConnected: isConnected,
        });
      }
      return { ...state, servers: newServers };
    }),
}));
