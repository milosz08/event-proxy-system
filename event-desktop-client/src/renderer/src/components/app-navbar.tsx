import { Alignment, Button, Classes, Navbar, NavbarDivider, NavbarGroup } from '@blueprintjs/core';
import ServerOverflowSelector from '@renderer/components/server-overflow-selector';
import { useAppStore } from '@renderer/store/use-app-store';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const AppNavbar: React.FC = (): React.ReactElement => {
  const { servers, activeSessions, openServersDrawer, openAddServerDrawer } = useAppStore();

  const { connected, all } = useMemo(() => {
    return { connected: activeSessions.size, all: servers.length };
  }, [activeSessions, servers]);

  return (
    <StyledNavbar>
      <StaticSection align={Alignment.START}>
        <Button
          className={Classes.MINIMAL}
          icon="cube"
          text={`Proxy servers (${connected}/${all})`}
          onClick={openServersDrawer}
        />
        <NavbarDivider />
      </StaticSection>
      <DynamicSection>{servers.length > 0 && <ServerOverflowSelector />}</DynamicSection>
      <StaticSection align={Alignment.END}>
        <Button icon="add" text="Add proxy server" onClick={openAddServerDrawer} />
      </StaticSection>
    </StyledNavbar>
  );
};

const StyledNavbar = styled(Navbar)`
  display: flex;
  align-items: center;
  width: 100%;
  padding-right: 15px;
`;

const StaticSection = styled(NavbarGroup)`
  flex: 0 0 auto;
  position: relative;
`;

const DynamicSection = styled.div`
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  margin: 0 15px;
  display: flex;
`;

export default AppNavbar;
