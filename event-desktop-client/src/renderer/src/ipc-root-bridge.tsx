import { useAsyncEffect } from '@reactuses/core';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useEffect, useMemo } from 'react';
import notificationSound from '../assets/notification.mp3';

const IpcRootBridge: React.FC = (): null => {
  const {
    uiConfig: { eventTable, eventSourceFilter, selectedServerId },
    setServers,
    setUiConfig,
    setActiveSessions,
    removeActiveSession,
    updateHeartbeat,
    insertLiveEvent,
    setUnreadNotifications,
  } = useAppStore();

  const audio = useMemo(() => new Audio(notificationSound), []);

  useEffect(() => {
    const onSseEvent = window.api.onSseEvent(async (serverId, payload, error) => {
      if (error) {
        await AppToaster.error(error);
        return;
      }
      if (!payload) {
        removeActiveSession(serverId);
        return;
      }
      if (serverId === selectedServerId) {
        audio.currentTime = 0;
        await audio.play();
        insertLiveEvent(payload);
      }
    });
    return () => onSseEvent();
  }, [
    audio,
    eventSourceFilter,
    eventTable,
    insertLiveEvent,
    removeActiveSession,
    selectedServerId,
  ]);

  useEffect(() => {
    const onActiveSessions = window.api.onActiveSessions(ids => {
      setActiveSessions(ids);
      return () => onActiveSessions();
    });
  }, [setActiveSessions]);

  useEffect(() => {
    const onHeartbeat = window.api.onHeartbeat(updateHeartbeat);
    return () => onHeartbeat();
  }, [updateHeartbeat]);

  useEffect(() => {
    const onSessionExpired = window.api.onSessionExpired(removeActiveSession);
    return () => onSessionExpired();
  }, [removeActiveSession]);

  useEffect(() => {
    const onBadgeSyncAll = window.api.onBadgeSyncAll(setUnreadNotifications);
    return () => onBadgeSyncAll();
  }, [setUnreadNotifications]);

  useAsyncEffect(
    async () => {
      setServers(await window.api.getServers());
      setUiConfig(await window.api.getUiConfig());
    },
    () => {},
    [setServers, setUiConfig]
  );

  return null;
};

export default IpcRootBridge;
