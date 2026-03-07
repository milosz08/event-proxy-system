import { ServerConfigDTO, UiConfig } from '@shared-types/shared';
import { create } from 'zustand';

export type ServerConfig = Omit<ServerConfigDTO, 'hasDefaultPassword'>;

type AppState = {
  // state
  servers: Map<string, Omit<ServerConfig, 'id'>>;
  uiConfig: UiConfig;
  activeSessions: Set<string>;
  selectedServerId: string | null;
  updateDefaultPasswordServerId: string | null;
  serversDrawerActive: boolean;
  addServerDrawerActive: boolean;
  selectedEvents: number[];
  // actions
  setServers: (servers: ServerConfig[]) => void;
  setUiConfig: (uiConfig: UiConfig) => void;
  selectServer: (id: string) => void;
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
  addSelectedEvents: (ids: number[]) => void;
  removeSelectedEvents: (ids: number[]) => void;
  updateHeartbeat: (serverId: string, status: boolean, resTimeMillis?: number) => void;
};

export const useAppStore = create<AppState>(set => ({
  // state
  servers: new Map(),
  uiConfig: {
    sideBySideLook: false,
    panelSizes: [50, 50],
  },
  activeSessions: new Set(),
  selectedServerId: null,
  updateDefaultPasswordServerId: null,
  serversDrawerActive: false,
  addServerDrawerActive: false,
  selectedEvents: [],
  // actions
  setServers: servers =>
    set(prevState => ({
      ...prevState,
      servers: new Map(servers.map(server => [server.id, server])),
    })),
  setUiConfig: uiConfig => set(prevState => ({ ...prevState, uiConfig })),
  selectServer: id => set({ selectedServerId: id }),
  removeServer: id =>
    set(prevState => {
      const newServers = new Map(prevState.servers);
      newServers.delete(id);
      return {
        ...prevState,
        servers: newServers,
        activeSessions: new Set(
          [...prevState.activeSessions].filter(sessionId => sessionId !== id)
        ),
        selectedServerId: prevState.selectedServerId === id ? null : prevState.selectedServerId,
      };
    }),
  setActiveSessions: ids => set({ activeSessions: new Set(ids) }),
  addActiveSession: id =>
    set(prevState => {
      const newSet = new Set(prevState.activeSessions);
      newSet.add(id);
      return { ...prevState, activeSessions: newSet };
    }),
  removeActiveSession: id =>
    set(prevState => {
      const newSet = new Set(prevState.activeSessions);
      newSet.delete(id);
      return {
        ...prevState,
        activeSessions: newSet,
        ...(prevState.selectedServerId === id ? { selectedServerId: null } : {}),
      };
    }),
  openDefaultPasswordDialog: serverId => set({ updateDefaultPasswordServerId: serverId }),
  closeDefaultPasswordDialog: () => set({ updateDefaultPasswordServerId: null }),
  openServersDrawer: () => set({ serversDrawerActive: true }),
  closeServersDrawer: () => set({ serversDrawerActive: false }),
  openAddServerDrawer: () => set({ addServerDrawerActive: true }),
  closeAddServerDrawer: () => set({ addServerDrawerActive: false }),
  addSelectedEvents: ids =>
    set(prevState => ({
      ...prevState,
      selectedEvents: [...ids, ...prevState.selectedEvents],
    })),
  removeSelectedEvents: ids =>
    set(prevState => ({
      ...prevState,
      selectedEvents: prevState.selectedEvents.filter(eventId => ids.includes(eventId)),
    })),
  updateHeartbeat: (serverId, status, resTimeMillis) =>
    set(prevState => {
      if (!prevState.servers.has(serverId)) {
        return prevState;
      }
      const newServers = new Map(prevState.servers);
      const existingServer = newServers.get(serverId);
      if (!existingServer) {
        return prevState;
      }
      newServers.set(serverId, {
        ...existingServer,
        lastHeartbeatTimestamp: Date.now(),
        lastHeartbeatStatus: status,
        lastHeartbeatResTimeMillis: resTimeMillis,
      });
      return {
        ...prevState,
        servers: newServers,
      };
    }),
}));
