import { create } from 'zustand';

export type ServerConfig = {
  id: string;
  name: string;
  url: string;
  username: string;
  unreadNotifications: number;
};

type AppState = {
  // state
  servers: Map<string, Omit<ServerConfig, 'id'>>;
  activeSessions: Set<string>;
  selectedServerId: string | null;
  updateDefaultPasswordServerId: string | null;
  serversDrawerActive: boolean;
  addServerDrawerActive: boolean;
  // actions
  setServers: (servers: ServerConfig[]) => void;
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
};

export const useAppStore = create<AppState>(set => ({
  // state
  servers: new Map(),
  activeSessions: new Set(),
  selectedServerId: null,
  updateDefaultPasswordServerId: null,
  serversDrawerActive: false,
  addServerDrawerActive: false,
  // actions
  setServers: servers =>
    set(prevState => ({
      ...prevState,
      servers: new Map(servers.map(server => [server.id, server])),
    })),
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
}));
