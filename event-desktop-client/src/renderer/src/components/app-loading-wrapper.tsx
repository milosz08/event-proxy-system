import { Spinner } from '@blueprintjs/core';
import { useAppStore } from '@renderer/store/use-app-store';
import React from 'react';
import styled from 'styled-components';

type Props = {
  children: React.ReactElement;
};

const AppLoadingWrapper: React.FC<Props> = ({ children }): React.ReactElement => {
  const { uiIsLoading } = useAppStore();

  if (uiIsLoading) {
    return (
      <LoaderContainer>
        <Spinner size={40} />
      </LoaderContainer>
    );
  }

  return <>{children}</>;
};

const LoaderContainer = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
`;

export default AppLoadingWrapper;
