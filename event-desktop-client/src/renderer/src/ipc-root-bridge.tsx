import { useAppStore } from '@renderer/store/use-app-store';
import React, { useEffect } from 'react';

const IpcRootBridge: React.FC = (): null => {
  const { setServers, setActiveSessions, removeActiveSession, selectServer, updateHeartbeat } =
    useAppStore();

  useEffect(() => {
    window.api.getServers().then(setServers);
    const onSessionExpired = window.api.onSessionExpired(id => {
      removeActiveSession(id);
    });
    const onActiveSessions = window.api.onActiveSessions(ids => {
      setActiveSessions(ids);
      if (ids.length > 0) {
        // select first active server at startup, may be changed to remembered before closed up app
        selectServer(ids[0]);
      }
    });
    const onHeartbeat = window.api.onHeartbeat(updateHeartbeat);
    return () => {
      onSessionExpired();
      onActiveSessions();
      onHeartbeat();
    };
  }, [removeActiveSession, selectServer, setActiveSessions, setServers, updateHeartbeat]);

  return null;
};

export default IpcRootBridge;
