import { Divider } from '@blueprintjs/core';
import EventDetails from '@renderer/components/event-details';
import EventsTable from '@renderer/components/events-table';
import ProxyServerWaitingScreen from '@renderer/components/proxy-server-waiting-screen';
import SplitPanelsLayout from '@renderer/components/split-panels-layout';
import Toolbar from '@renderer/components/toolbar';
import useConnectionStatus from '@renderer/hooks/use-connection-status';
import useSpinner from '@renderer/hooks/use-spinner';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const ServerEventsContent: React.FC = (): React.ReactElement | null => {
  const {
    servers,
    activeSessions,
    uiConfig,
    openDefaultPasswordDialog,
    addActiveSession,
    setUiConfig,
    setSelectedEvents,
  } = useAppStore();
  const { showDetails, selectedServerId } = uiConfig;

  const [loadingConnect, runConnect] = useSpinner();

  const server = useMemo(
    () => (selectedServerId ? servers.get(selectedServerId) : undefined),
    [servers, selectedServerId]
  );
  const selectedServerName = server?.name;

  const [getConnectionStatus] = useConnectionStatus();
  const connectionStatus = getConnectionStatus(selectedServerId);

  const onSelectFirstConnected = async (): Promise<void> => {
    if (activeSessions.size < 1) {
      await AppToaster.error('None connected servers');
      return;
    }
    const [firstActiveSession] = activeSessions;
    setSelectedEvents([]);
    setUiConfig({ ...uiConfig, selectedServerId: firstActiveSession });
    setUiConfig(await window.api.updateUiConfig({ selectedServerId: firstActiveSession }));
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
        const { success, error, data } = await window.api.connect(selectedServerId);
        if (success && data) {
          if (data.hasDefaultPassword) {
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

  if (connectionStatus === 'disconnected') {
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

  if (connectionStatus === 'reconnecting') {
    return (
      <ProxyServerWaitingScreen
        title={`Connecting to ${selectedServerName}...`}
        description="This proxy server is temporarily unreachable, retrying in the background..."
        loadingConnect={true}
        actionDescription="Connecting..."
        onAction={async () => {}}
      />
    );
  }

  return (
    <MainContent>
      <Toolbar />
      <Divider compact />
      <SplitPanelsLayout
        firstPanelContent={<EventsTable />}
        secondPanelContent={showDetails ? <EventDetails /> : null}
      />
    </MainContent>
  );
};

const MainContent = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
`;

export default ServerEventsContent;
