import { Colors, Icon, Intent } from '@blueprintjs/core';
import React from 'react';
import styled, { css, keyframes } from 'styled-components';

type Props = {
  isConnected: boolean;
  size?: number;
  noMarginRight?: boolean;
  noMarginLeft?: boolean;
};

const PulsingIcon: React.FC<Props> = ({
  isConnected,
  size = 12,
  noMarginRight = false,
  noMarginLeft = false,
}): React.ReactElement => {
  return (
    <PulsingContainer $noMarginRight={noMarginRight} $noMarginLeft={noMarginLeft}>
      <PulsingWrapper $isConnected={isConnected}>
        <ConnectionIcon
          icon="symbol-circle"
          intent={isConnected ? Intent.SUCCESS : Intent.NONE}
          size={size}
        />
      </PulsingWrapper>
    </PulsingContainer>
  );
};

const pulseRippleAnimation = keyframes`
  0% {
    transform: scale(0.8);
    opacity: 0.8;
  }
  50% {
    opacity: 0.4;
  }
  100% {
    transform: scale(1.6);
    opacity: 0;
  }
`;

const PulsingContainer = styled.div<{ $noMarginRight: boolean; $noMarginLeft: boolean }>`
  margin-right: ${props => (props.$noMarginRight ? '0px' : '10px')};
  margin-left: ${props => (props.$noMarginLeft ? '0px' : '5px')};
  display: flex;
  align-items: center;
`;

const PulsingWrapper = styled.div<{ $isConnected: boolean }>`
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 4px;
  ${props =>
    props.$isConnected &&
    css`
      &::after {
        content: '';
        position: absolute;
        width: 14px;
        height: 14px;
        background-color: ${Colors.GREEN3};
        border-radius: 50%;
        z-index: 0;
        animation: ${pulseRippleAnimation} 2s ease-out infinite;
      }
    `}
`;

const ConnectionIcon = styled(Icon)`
  position: relative;
  z-index: 1;
`;

export default PulsingIcon;
