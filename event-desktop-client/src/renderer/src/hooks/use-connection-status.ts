import { Colors, Intent } from '@blueprintjs/core';
import { useAppStore } from '@renderer/store/use-app-store';

export type ConnectionStatus = 'connected' | 'reconnecting' | 'disconnected' | 'unknown';

const intents: Record<ConnectionStatus, Intent> = {
  connected: Intent.SUCCESS,
  reconnecting: Intent.WARNING,
  disconnected: Intent.DANGER,
  unknown: Intent.NONE,
};

const colors: Record<ConnectionStatus, string> = {
  connected: Colors.GREEN3,
  reconnecting: Colors.ORANGE3,
  disconnected: Colors.RED3,
  unknown: '',
};

const useConnectionStatus = (): [
  (serverId: string | null) => ConnectionStatus,
  (serverId: string | null) => Intent,
  (serverId: string | null) => string,
] => {
  const { servers, activeSessions } = useAppStore();

  const getConnectionStatus = (serverId: string | null): ConnectionStatus => {
    if (serverId == null) {
      return 'unknown';
    }
    const selectedServer = servers.get(serverId);
    if (!serverId || !selectedServer || !activeSessions.has(serverId)) {
      return 'disconnected';
    }
    if (selectedServer.lastHeartbeatStatus === false || selectedServer.sseConnected !== true) {
      return 'reconnecting';
    }
    return 'connected';
  };

  const getConnectionIntent = (serverId: string | null): Intent => {
    const status = getConnectionStatus(serverId);
    return intents[status];
  };

  const getConnectionColor = (serverId: string | null): string => {
    const status = getConnectionStatus(serverId);
    return colors[status];
  };

  return [getConnectionStatus, getConnectionIntent, getConnectionColor];
};

export default useConnectionStatus;
