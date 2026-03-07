import ProxyServerWaitingScreen from '@renderer/components/proxy-server-waiting-screen';
import useSpinner from '@renderer/hooks/use-spinner';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const ServerEventsContent: React.FC = (): React.ReactElement | null => {
  const {
    servers,
    selectedServerId,
    activeSessions,
    selectServer,
    openDefaultPasswordDialog,
    addActiveSession,
  } = useAppStore();

  const [loadingConnect, runConnect] = useSpinner();

  const selectedServerName = useMemo(
    () => (selectedServerId ? servers.get(selectedServerId)?.name : undefined),
    [servers, selectedServerId]
  );

  const onSelectFirstConnected = async (): Promise<void> => {
    if (activeSessions.size < 1) {
      await AppToaster.error('None connected servers');
      return;
    }
    const [firstActiveSession] = activeSessions;
    selectServer(firstActiveSession);
  };

  if (!selectedServerId || !selectedServerName) {
    return (
      <ProxyServerWaitingScreen
        title="Not following any server"
        description="You do not selected any proxy server to follow."
        loadingConnect={loadingConnect}
        actionDescription="Select first connected server"
        onAction={onSelectFirstConnected}
      />
    );
  }

  const connectToServer = async (): Promise<void> => {
    await runConnect(
      () => true,
      async () => {
        const { success, error, hasDefaultPassword } = await window.api.connect(selectedServerId);
        if (success) {
          if (hasDefaultPassword) {
            openDefaultPasswordDialog(selectedServerId);
          }
          addActiveSession(selectedServerId);
          await AppToaster.success('Connected to the server');
        }
        if (error) {
          await AppToaster.error(error);
        }
      }
    );
  };

  if (!activeSessions.has(selectedServerId)) {
    return (
      <ProxyServerWaitingScreen
        title={`Not connected to server ${selectedServerName}`}
        description="You cannot connected to followed proxy server."
        loadingConnect={loadingConnect}
        actionDescription="Connect to server"
        onAction={connectToServer}
      />
    );
  }

  return <MainContent>SERVER: {selectedServerName} MESSAGES CONTENT</MainContent>;
};

const MainContent = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow-y: hidden;
`;

export default ServerMessagesContent;
