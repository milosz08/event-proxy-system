import {
  Button,
  Classes,
  Icon,
  Menu,
  MenuItem,
  OverflowList,
  PopoverNext,
  Tag,
} from '@blueprintjs/core';
import PulsingIcon from '@renderer/components/pulsing-icon';
import useConnectionStatus from '@renderer/hooks/use-connection-status';
import { ServerConfig, useAppStore } from '@renderer/store/use-app-store';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const ServerOverflowSelector: React.FC = (): React.ReactElement => {
  const { servers, uiConfig, activeSessions, unreadNotifications, setUiConfig, setSelectedEvents } =
    useAppStore();
  const { selectedServerId } = uiConfig;

  const onSelectServer = async (serverId: string): Promise<void> => {
    setSelectedEvents([]);
    setUiConfig({ ...uiConfig, selectedServerId: serverId });
    setUiConfig(await window.api.updateUiConfig({ selectedServerId: serverId }));
  };

  const [getConnectionStatus, getConnectionIntent, getConnectionColor] = useConnectionStatus();

  const ServerTag = (server: ServerConfig): React.ReactElement | null => {
    const unreadCount = unreadNotifications[server.id] ?? 0;
    return activeSessions.has(server.id) && unreadCount > 0 ? (
      <Tag round intent="danger" minimal>
        {unreadCount > 9 ? '9+' : unreadCount}
      </Tag>
    ) : null;
  };

  const serversList = useMemo(
    () =>
      [...servers].map(([id, config]) => ({
        id,
        ...config,
      })),
    [servers]
  );

  return (
    <OverflowList
      items={serversList}
      visibleItemRenderer={server => (
        <Button
          key={server.id}
          active={selectedServerId === server.id}
          onClick={() => onSelectServer(server.id)}
          text={server.name}
          title={server.url}
          icon={
            <PulsingIcon
              status={getConnectionStatus(server.id)}
              intent={getConnectionIntent(server.id)}
              pulseColor={getConnectionColor(server.id)}
            />
          }
          endIcon={<ServerTag {...server} />}
        />
      )}
      overflowRenderer={overflowItems => (
        <PopoverNext
          content={
            <Menu>
              {overflowItems.map(server => (
                <MenuItem
                  key={server.id}
                  text={
                    <MenuItemTextContainer>
                      <SelectorIcon
                        icon="symbol-circle"
                        intent={getConnectionIntent(server.id)}
                        size={12}
                      />
                      {server.name}
                    </MenuItemTextContainer>
                  }
                  icon={selectedServerId === server.id ? 'tick' : 'blank'}
                  onClick={() => onSelectServer(server.id)}
                  labelElement={<ServerTag {...server} />}
                />
              ))}
            </Menu>
          }
          placement="bottom-start">
          <Button icon="more" />
        </PopoverNext>
      )}
      collapseFrom="end"
      minVisibleItems={0}
      className={Classes.BUTTON_GROUP}
    />
  );
};

const MenuItemTextContainer = styled('div')`
  display: flex;
  align-items: center;
`;

const SelectorIcon = styled(Icon)`
  margin-right: 8px;
`;

export default ServerOverflowSelector;
