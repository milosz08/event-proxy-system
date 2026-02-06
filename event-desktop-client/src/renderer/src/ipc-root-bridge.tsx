import { useAppStore } from '@renderer/store/use-app-store';
import React, { useEffect } from 'react';

const IpcRootBridge: React.FC = (): null => {
  const { setServers, setActiveSessions, removeActiveSession, selectServer } = useAppStore();

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
    return () => {
      onSessionExpired();
      onActiveSessions();
    };
  }, [removeActiveSession, selectServer, setActiveSessions, setServers]);

  return null;
};

export default IpcRootBridge;
