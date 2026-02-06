import { FocusStyleManager } from '@blueprintjs/core';
import AddServerDrawer from '@renderer/components/add-server-drawer';
import AppNavbar from '@renderer/components/app-navbar';
import ChangeDefaultPasswordPopup from '@renderer/components/change-default-password-popup';
import ServerMessagesContent from '@renderer/components/server-messages-content';
import ServersDrawer from '@renderer/components/servers-drawer';
import IpcRootBridge from '@renderer/ipc-root-bridge';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import styled from 'styled-components';
import './main.css';

FocusStyleManager.onlyShowFocusOnTabs();

const appMount = document.getElementById('app-mount')!;

const AppContainer = styled.div`
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
`;

createRoot(appMount).render(
  <StrictMode>
    <IpcRootBridge />
    <AppContainer>
      <AppNavbar />
      <ServerMessagesContent />
      <ServersDrawer />
      <AddServerDrawer />
      <ChangeDefaultPasswordPopup />
    </AppContainer>
  </StrictMode>
);
