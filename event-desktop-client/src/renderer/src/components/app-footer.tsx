import { Colors } from '@blueprintjs/core';
import PulsingIcon from '@renderer/components/pulsing-icon';
import useConnectionStatus from '@renderer/hooks/use-connection-status';
import { useAppStore } from '@renderer/store/use-app-store';
import { formatTimestamp } from '@renderer/utils/utils';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const AppFooter: React.FC = (): React.ReactElement => {
  const {
    servers,
    uiConfig: { selectedServerId },
  } = useAppStore();

  const selectedServer = useMemo(
    () => servers.get(selectedServerId as string),
    [servers, selectedServerId]
  );

  const [getConnectionStatus, getConnectionIntent, getConnectionColor] = useConnectionStatus();
  const connectionStatus = getConnectionStatus(selectedServerId);
  const isConnected = connectionStatus === 'connected';

  const { statusText, statusColor } = useMemo(() => {
    switch (connectionStatus) {
      case 'connected':
        return { statusText: 'OK', statusColor: Colors.GREEN5 };
      case 'reconnecting':
        return { statusText: 'RECONNECTING', statusColor: Colors.ORANGE5 };
      case 'disconnected':
      default:
        return { statusText: 'DISCONNECTED', statusColor: Colors.GRAY4 };
    }
  }, [connectionStatus]);

  return (
    <FooterContainer>
      <LeftSection>
        <LineContent>
          <PulsingIcon
            status={connectionStatus}
            intent={getConnectionIntent(selectedServerId)}
            pulseColor={getConnectionColor(selectedServerId)}
            noMarginLeft
          />
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
