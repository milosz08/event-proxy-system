import {
  Button,
  Classes,
  Icon,
  Intent,
  Menu,
  MenuItem,
  OverflowList,
  Popover,
  Position,
  Tag,
} from '@blueprintjs/core';
import { ServerConfig, useAppStore } from '@renderer/store/use-app-store';
import React from 'react';
import styled from 'styled-components';

const ServerOverflowSelector: React.FC = (): React.ReactElement => {
  const { servers, activeSessions, selectedServerId, selectServer } = useAppStore();

  const ServerTag = (server: ServerConfig): React.ReactElement | null =>
    selectedServerId && activeSessions.has(selectedServerId) && server.unreadNotifications > 0 ? (
      <Tag round intent="danger" minimal>
        {server.unreadNotifications > 9 ? '9+' : server.unreadNotifications}
      </Tag>
    ) : null;

  return (
    <OverflowList
      items={servers}
      visibleItemRenderer={server => (
        <Button
          key={server.id}
          active={selectedServerId === server.id}
          onClick={() => selectServer(server.id)}
          text={server.name}
          title={server.url}
          icon={
            <SelectorIcon
              icon="symbol-circle"
              intent={activeSessions.has(server.id) ? Intent.SUCCESS : Intent.NONE}
              size={12}
            />
          }
          endIcon={<ServerTag {...server} />}
        />
      )}
      overflowRenderer={overflowItems => (
        <Popover
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
                  onClick={() => selectServer(server.id)}
                  labelElement={<ServerTag {...server} />}
                />
              ))}
            </Menu>
          }
          position={Position.BOTTOM_LEFT}>
          <Button icon="more" />
        </Popover>
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
