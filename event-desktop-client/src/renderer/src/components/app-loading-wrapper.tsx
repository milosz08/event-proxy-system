import { Colors, Spinner, Text } from '@blueprintjs/core';
import { useAppStore } from '@renderer/store/use-app-store';
import React, { useEffect, useState } from 'react';
import styled from 'styled-components';

type Props = {
  children: React.ReactElement;
};

const AppLoadingWrapper: React.FC<Props> = ({ children }): React.ReactElement => {
  const { uiIsLoading } = useAppStore();

  const [serverName, setServerName] = useState<string | undefined>();
  const [msg, setMsg] = useState<string | undefined>();

  useEffect(() => {
    const unsubSetupPoint = window.api.onSetupPoint((serverName, msg) => {
      setServerName(serverName);
      setMsg(msg);
    });
    return () => unsubSetupPoint();
  }, []);

  if (uiIsLoading) {
    return (
      <LoaderContainer>
        <Spinner size={40} />
        <SetupPointText>{msg && (serverName && `[${serverName}]: `) + msg + '...'}</SetupPointText>
      </LoaderContainer>
    );
  }

  return <>{children}</>;
};

const LoaderContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: 20px;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
`;

const SetupPointText = styled(Text)`
  color: ${Colors.GRAY4};
  font-size: 1rem;
`;

export default AppLoadingWrapper;
