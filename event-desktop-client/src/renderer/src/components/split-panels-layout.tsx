import { Divider } from '@blueprintjs/core';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useCallback } from 'react';
import { Group, Panel, Separator } from 'react-resizable-panels';
import styled from 'styled-components';

type Props = {
  firstPanelContent: React.ReactNode;
  secondPanelContent: React.ReactNode | null;
  defaultSizes?: number[];
};

const SplitPanelsLayout: React.FC<Props> = ({
  firstPanelContent,
  secondPanelContent,
  defaultSizes = [50, 50],
}): React.ReactElement => {
  const { uiConfig, setUiConfig } = useAppStore();
  const initSizes = uiConfig.panelSizes || defaultSizes;

  const handleLayoutChanged = useCallback(
    (layout: { [panelId: string]: number }): void => {
      const leftSize = layout['panel-1'];
      const rightSize = layout['panel-2'];
      if (leftSize === undefined || rightSize === undefined) {
        return;
      }
      const newPanelSizes = [leftSize, rightSize];
      setUiConfig({ ...uiConfig, panelSizes: newPanelSizes });
      window.api
        .updateUiConfig({ panelSizes: newPanelSizes })
        .then(config => setUiConfig(config))
        .catch(err => AppToaster.error(err));
    },
    [uiConfig, setUiConfig]
  );

  return (
    <LayoutContainer>
      <Group
        orientation={uiConfig.sideBySideLook ? 'horizontal' : 'vertical'}
        onLayoutChanged={handleLayoutChanged}>
        <Panel id="panel-1" defaultSize={initSizes[0]} minSize={20}>
          <ContentWrapper>{firstPanelContent}</ContentWrapper>
        </Panel>
        <StyledResizeHandle $isSideBySide={uiConfig.sideBySideLook}>
          <StyledDivider compact />
        </StyledResizeHandle>
        {secondPanelContent && (
          <Panel id="panel-2" defaultSize={initSizes[1]} minSize={20}>
            <ContentWrapper>{secondPanelContent}</ContentWrapper>
          </Panel>
        )}
      </Group>
    </LayoutContainer>
  );
};

const LayoutContainer = styled.div`
  flex: 1;
  display: flex;
  overflow: hidden;
`;

const StyledResizeHandle = styled(Separator)<{ $isSideBySide: boolean }>`
  flex: 0 0 auto;
  width: ${({ $isSideBySide }) => ($isSideBySide ? '1px' : '100%')};
  height: ${({ $isSideBySide }) => ($isSideBySide ? '100%' : '1px')};
  cursor: ${({ $isSideBySide }) => ($isSideBySide ? 'col-resize' : 'row-resize')};
`;

const StyledDivider = styled(Divider)`
  width: 100%;
  height: 100%;
`;

const ContentWrapper = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  position: relative;
  overflow: hidden;
`;

export default SplitPanelsLayout;
