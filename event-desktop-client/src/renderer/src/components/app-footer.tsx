import { Colors } from '@blueprintjs/core';
import PulsingIcon from '@renderer/components/pulsing-icon';
import { useAppStore } from '@renderer/store/use-app-store';
import { formatTimestamp } from '@renderer/utils/utils';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const AppFooter: React.FC = (): React.ReactElement => {
  const {
    servers,
    uiConfig: { selectedServerId },
    activeSessions,
  } = useAppStore();

  const selectedServer = useMemo(
    () => servers.get(selectedServerId as string),
    [servers, selectedServerId]
  );
  const isConnected = useMemo(
    () => !!(selectedServerId && selectedServer && activeSessions.has(selectedServerId)),
    [activeSessions, selectedServer, selectedServerId]
  );
  const { statusText, statusColor } = useMemo(
    () =>
      selectedServer?.lastHeartbeatStatus === undefined || !isConnected
        ? {
            statusText: 'UNKNOWN',
            statusColor: Colors.ORANGE5,
          }
        : selectedServer?.lastHeartbeatStatus
          ? {
              statusText: 'OK',
              statusColor: Colors.GREEN5,
            }
          : { statusText: 'INVALID', statusColor: Colors.RED5 },
    [isConnected, selectedServer?.lastHeartbeatStatus]
  );

  return (
    <FooterContainer>
      <LeftSection>
        <LineContent>
          <PulsingIcon isConnected={isConnected} noMarginLeft />
          {isConnected ? (
            <span>
              Server: <ContentText>{selectedServer?.url}</ContentText>
            </span>
          ) : (
            <span>No connection</span>
          )}
        </LineContent>
        <span>
          User: <ContentText>{selectedServer ? selectedServer?.username : '?'}</ContentText>
        </span>
      </LeftSection>
      <RightSection>
        <span>
          Last heartbeat:{' '}
          <ContentText>{formatTimestamp(selectedServer?.lastHeartbeatTimestamp)}</ContentText> (
          {selectedServer?.lastHeartbeatResTimeMillis?.toFixed(0) || '?'} ms)
        </span>
        <span>
          Status: <StatusText $color={statusColor}>{statusText}</StatusText>
        </span>
      </RightSection>
    </FooterContainer>
  );
};

const FooterContainer = styled.div`
  padding: 7px 20px;
  color: ${Colors.GRAY3};
  display: flex;
  align-items: center;
`;

const Section = styled.div`
  flex-basis: 50%;
  display: flex;
  gap: 30px;
`;

const LeftSection = styled(Section)`
  justify-content: start;
`;

const RightSection = styled(Section)`
  justify-content: end;
`;

const LineContent = styled.span`
  display: flex;
`;

const ContentText = styled.span`
  color: ${Colors.WHITE};
`;

const StatusText = styled.span<{ $color: string }>`
  color: ${props => props.$color};
`;

export default AppFooter;
