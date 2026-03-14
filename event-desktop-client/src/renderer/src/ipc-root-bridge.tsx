import { useAsyncEffect } from '@reactuses/core';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useEffect, useMemo } from 'react';
import notificationSound from '../assets/notification.mp3';

const IpcRootBridge: React.FC = () => {
  const audio = useMemo(() => new Audio(notificationSound), []);

  useEffect(() => {
    const unsubSse = window.api.onSseEvent(async (serverId, payload, error) => {
      if (error) {
        await AppToaster.error(error);
        return;
      }
      const state = useAppStore.getState();
      if (!payload) {
        state.removeActiveSession(serverId);
        return;
      }
      if (serverId === state.uiConfig.selectedServerId) {
        audio.currentTime = 0;
        await audio.play();
        state.insertLiveEvent(payload);
      }
    });
    const unsubHeartbeat = window.api.onHeartbeat((serverId, status, resTime) => {
      useAppStore.getState().updateHeartbeat(serverId, status, resTime);
    });
    const unsubExpired = window.api.onSessionExpired(serverId => {
      useAppStore.getState().removeActiveSession(serverId);
    });
    const unsubBadge = window.api.onBadgeSyncAll(counts => {
      useAppStore.getState().setUnreadNotifications(counts);
    });
    return () => {
      unsubSse();
      unsubHeartbeat();
      unsubExpired();
      unsubBadge();
    };
  }, [audio]);

  useAsyncEffect(
    async () => {
      const state = useAppStore.getState();
      state.setServers(await window.api.getServers());
      state.setUiConfig(await window.api.getUiConfig());
      state.setUnreadNotifications(await window.api.getAllBadgeCounts());
      state.setActiveSessions(await window.api.getInitialSessions());
    },
    () => {},
    []
  );

  return null;
};

export default IpcRootBridge;
