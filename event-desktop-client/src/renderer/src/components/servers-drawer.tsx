import {
  Button,
  Card,
  Classes,
  Drawer,
  DrawerSize,
  Elevation,
  Icon,
  Intent,
  NonIdealState,
  NonIdealStateIconSize,
} from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import ConfirmAlert from '@renderer/components/confirm-alert';
import useSpinner from '@renderer/hooks/use-spinner';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useMemo, useState } from 'react';
import styled from 'styled-components';

const ServersDrawer: React.FC = (): React.ReactElement => {
  const {
    servers,
    activeSessions,
    serversDrawerActive,
    closeServersDrawer,
    openAddServerDrawer,
    addActiveSession,
    removeActiveSession,
    removeServer,
    openDefaultPasswordDialog,
  } = useAppStore();

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [disconnectOpen, setDisconnectOpen] = useState(false);
  const [serverId, setServerId] = useState<string | null>(null);

  const [loadingConnect, runConnect] = useSpinner();
  const [loadingDisconnect, runDisconnect] = useSpinner();
  const [loadingDelete, runDelete] = useSpinner();

  const serverName = useMemo(() => servers.find(s => s.id === serverId)?.name, [servers, serverId]);
  const { connected, all } = useMemo(() => {
    return { connected: activeSessions.size, all: servers.length };
  }, [activeSessions, servers]);

  const handleServerConnect = async (id: string): Promise<void> => {
    await runConnect(
      () => true,
      async () => {
        const { success, error, hasDefaultPassword } = await window.api.connect(id);
        if (success) {
          if (hasDefaultPassword) {
            openDefaultPasswordDialog(id);
          }
          addActiveSession(id);
          await AppToaster.success('Connected to the server');
        }
        if (error) {
          await AppToaster.error(error);
        }
      }
    );
  };

  const handleDisconnectServer = async (): Promise<void> => {
    await runDisconnect(
      () => !!(serverId && activeSessions.has(serverId)),
      async () => {
        await window.api.disconnect(serverId as string);
        removeActiveSession(serverId as string);
        setServerId(null);
        await AppToaster.success('Disconnected with server');
        setDisconnectOpen(false);
      }
    );
  };

  const handleDeleteServer = async (): Promise<void> => {
    await runDelete(
      () => !!(serverId && !activeSessions.has(serverId)),
      async () => {
        const { success, error } = await window.api.removeServer(serverId as string);
        if (success) {
          removeServer(serverId as string);
          setServerId(null);
          await AppToaster.success('Deleted server');
        }
        if (error) {
          await AppToaster.error(error);
        }
        setDeleteOpen(false);
      }
    );
  };

  return (
    <Drawer
      isOpen={serversDrawerActive}
      onClose={closeServersDrawer}
      title={`Proxy servers (${connected}/${all})`}
      icon="cube"
      position="left"
      size={DrawerSize.SMALL}>
      <div className={Classes.DRAWER_BODY}>
        <div className={Classes.DIALOG_BODY}>
          {servers.length === 0 && (
            <NonIdealState
              action={
                <Button
                  text="Add server"
                  icon="plus"
                  variant="outlined"
                  onClick={openAddServerDrawer}
                />
              }
              description="Not found any proxy server."
              icon="search"
              iconSize={NonIdealStateIconSize.STANDARD}
              title="No results"
            />
          )}
          {servers.map(({ id, name, url, username }) => (
            <ServerCard key={id} compact elevation={Elevation.ZERO}>
              <CardHeader>
                <CardIcon
                  icon="symbol-circle"
                  intent={activeSessions.has(id) ? Intent.SUCCESS : Intent.NONE}
                  size={12}
                />
                <ServerName>{name}</ServerName>
              </CardHeader>
              <ServerInfo>
                <InfoRow>URL: {url}</InfoRow>
                <InfoRow>Username: {username}</InfoRow>
              </ServerInfo>
              <CardActions>
                <Button
                  size="small"
                  variant="outlined"
                  loading={loadingConnect && serverId === id}
                  intent={activeSessions.has(id) ? Intent.WARNING : Intent.SUCCESS}
                  text={activeSessions.has(id) ? 'Disconnect' : 'Connect'}
                  fill
                  onClick={async () => {
                    setServerId(id);
                    if (activeSessions.has(id)) {
                      setDisconnectOpen(true);
                    } else {
                      await handleServerConnect(id);
                    }
                  }}
                />
                <Button
                  size="small"
                  icon="trash"
                  intent={Intent.DANGER}
                  disabled={activeSessions.has(id)}
                  onClick={() => {
                    setServerId(id);
                    setDeleteOpen(true);
                  }}
                />
              </CardActions>
            </ServerCard>
          ))}
        </div>
      </div>
      <ConfirmAlert
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => handleDeleteServer()}
        loading={loadingDelete}
        icon={IconNames.TRASH}
        intent={Intent.DANGER}
        confirmButtonText="Delete">
        Are you sure want to delete server <b>{serverName}</b>? You not be able to connect with this
        proxy server anymore until you add it again.
      </ConfirmAlert>
      <ConfirmAlert
        isOpen={disconnectOpen}
        onClose={() => setDisconnectOpen(false)}
        onConfirm={() => handleDisconnectServer()}
        loading={loadingDisconnect}
        icon={IconNames.TH_DISCONNECT}
        intent={Intent.WARNING}
        confirmButtonText="Disconnect">
        Are you sure want to disconnect from <b>{serverName}</b>? After disconnect, you cannot
        receive notifications.
      </ConfirmAlert>
    </Drawer>
  );
};

const ServerCard = styled(Card)`
  margin-bottom: 10px;
`;

const CardHeader = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 5px;
`;

const CardIcon = styled(Icon)`
  margin-right: 8px;
`;

const ServerName = styled.div.attrs({ className: Classes.TEXT_OVERFLOW_ELLIPSIS })`
  font-weight: 600;
`;

const ServerInfo = styled.div.attrs({ className: Classes.TEXT_SMALL })`
  margin-bottom: 12px;
`;

const InfoRow = styled.div.attrs({ className: Classes.TEXT_OVERFLOW_ELLIPSIS })``;

const CardActions = styled.div`
  display: flex;
  gap: 10px;
  margin-top: auto;
`;

export default ServersDrawer;
