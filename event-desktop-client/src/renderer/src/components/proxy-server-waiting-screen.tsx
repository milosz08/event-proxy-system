import { Button, Classes, NonIdealState, Spinner } from '@blueprintjs/core';
import React from 'react';
import styled from 'styled-components';

type Props = {
  title: string;
  description: string;
  loadingConnect: boolean;
  actionDescription: string;
  onAction: () => Promise<void>;
};

const ProxyServerWaitingScreen: React.FC<Props> = ({
  title,
  description,
  loadingConnect,
  actionDescription,
  onAction,
}): React.ReactElement => (
  <MainCenteredContent>
    <NonIdealState
      action={
        <Button
          text={<TruncatedText>{actionDescription}</TruncatedText>}
          icon="plus"
          variant="outlined"
          loading={loadingConnect}
          className={Classes.TEXT_OVERFLOW_ELLIPSIS}
          onClick={onAction}
        />
      }
      description={description}
      icon={<Spinner size={30} />}
      iconSize={20}
      title={<TruncatedText $maxWidth={400}>{title}</TruncatedText>}
    />
  </MainCenteredContent>
);

const MainCenteredContent = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
`;

const TruncatedText = styled.div.attrs({ className: Classes.TEXT_OVERFLOW_ELLIPSIS })<{
  $maxWidth?: number;
}>`
  max-width: ${props => props.$maxWidth || 200}px;
  display: block;
`;

export default ProxyServerWaitingScreen;
