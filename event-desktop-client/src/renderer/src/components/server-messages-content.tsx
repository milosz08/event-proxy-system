import ProxyServerWaitingScreen from '@renderer/components/proxy-server-waiting-screen';
import useSpinner from '@renderer/hooks/use-spinner';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const ServerMessagesContent: React.FC = (): React.ReactElement | null => {
  const {
    servers,
    selectedServerId,
    activeSessions,
    selectServer,
    openDefaultPasswordDialog,
    addActiveSession,
  } = useAppStore();

  const [loadingConnect, runConnect] = useSpinner();

  const selectedServer = useMemo(
    () => servers.find(s => s.id === selectedServerId),
    [servers, selectedServerId]
  );

  const onSelectFirstConnected = async (): Promise<void> => {
    if (activeSessions.size < 1) {
      await AppToaster.error('None connected servers');
      return;
    }
    selectServer(activeSessions[0]);
  };

  if (!selectedServer) {
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
        const { success, error, hasDefaultPassword } = await window.api.connect(selectedServer.id);
        if (success) {
          if (hasDefaultPassword) {
            openDefaultPasswordDialog(selectedServer.id);
          }
          addActiveSession(selectedServer.id);
          await AppToaster.success('Connected to the server');
        }
        if (error) {
          await AppToaster.error(error);
        }
      }
    );
  };

  if (!activeSessions.has(selectedServer.id)) {
    return (
      <ProxyServerWaitingScreen
        title={`Not connected to server ${selectedServer.name}`}
        description="You cannot connected to followed proxy server."
        loadingConnect={loadingConnect}
        actionDescription={`Connect to server ${selectedServer.name}`}
        onAction={connectToServer}
      />
    );
  }

  return <MainContent>SERVER: {selectedServer.name} MESSAGES CONTENT</MainContent>;
};

const MainContent = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow-y: hidden;
`;

export default ServerMessagesContent;
