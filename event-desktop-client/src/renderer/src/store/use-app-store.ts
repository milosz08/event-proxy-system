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
  servers: ServerConfig[];
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
  servers: [],
  activeSessions: new Set(),
  selectedServerId: null,
  updateDefaultPasswordServerId: null,
  serversDrawerActive: false,
  addServerDrawerActive: false,
  // actions
  setServers: servers => set({ servers }),
  selectServer: id => set({ selectedServerId: id }),
  removeServer: id =>
    set(state => ({
      servers: state.servers.filter(s => s.id !== id),
      activeSessions: new Set([...state.activeSessions].filter(sessionId => sessionId !== id)),
      selectedServerId: state.selectedServerId === id ? null : state.selectedServerId,
    })),
  setActiveSessions: ids => set({ activeSessions: new Set(ids) }),
  addActiveSession: id =>
    set(state => {
      const newSet = new Set(state.activeSessions);
      newSet.add(id);
      return { activeSessions: newSet };
    }),
  removeActiveSession: id =>
    set(state => {
      const newSet = new Set(state.activeSessions);
      newSet.delete(id);
      return {
        activeSessions: newSet,
        ...(state.selectedServerId === id ? { selectedServerId: null } : {}),
      };
    }),
  openDefaultPasswordDialog: serverId => set({ updateDefaultPasswordServerId: serverId }),
  closeDefaultPasswordDialog: () => set({ updateDefaultPasswordServerId: null }),
  openServersDrawer: () => set({ serversDrawerActive: true }),
  closeServersDrawer: () => set({ serversDrawerActive: false }),
  openAddServerDrawer: () => set({ addServerDrawerActive: true }),
  closeAddServerDrawer: () => set({ addServerDrawerActive: false }),
}));
