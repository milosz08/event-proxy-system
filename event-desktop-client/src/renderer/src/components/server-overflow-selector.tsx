import {
  Button,
  Classes,
  Icon,
  Intent,
  Menu,
  MenuItem,
  OverflowList,
  PopoverNext,
  Tag,
} from '@blueprintjs/core';
import PulsingIcon from '@renderer/components/pulsing-icon';
import { ServerConfig, useAppStore } from '@renderer/store/use-app-store';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const ServerOverflowSelector: React.FC = (): React.ReactElement => {
  const { servers, uiConfig, activeSessions, setUiConfig } = useAppStore();
  const { selectedServerId } = uiConfig;

  const onSelectServer = async (serverId: string): Promise<void> => {
    setUiConfig({ ...uiConfig, selectedServerId: serverId });
    setUiConfig(await window.api.updateUiConfig({ selectedServerId: serverId }));
  };

  const ServerTag = (server: ServerConfig): React.ReactElement | null =>
    selectedServerId && activeSessions.has(selectedServerId) && server.unreadNotifications > 0 ? (
      <Tag round intent="danger" minimal>
        {server.unreadNotifications > 9 ? '9+' : server.unreadNotifications}
      </Tag>
    ) : null;

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
          icon={<PulsingIcon isConnected={activeSessions.has(server.id)} />}
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
                        intent={activeSessions.has(server.id) ? Intent.SUCCESS : Intent.NONE}
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
