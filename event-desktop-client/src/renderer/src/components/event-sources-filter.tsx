import { Button, Menu, MenuDivider, MenuItem, PopoverNext, Spinner } from '@blueprintjs/core';
import { useAsyncEffect } from '@reactuses/core';
import useSpinner from '@renderer/hooks/use-spinner';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useState } from 'react';
import styled from 'styled-components';

const EventSourceFilter: React.FC = () => {
  const { selectedServerId, uiConfig, setUiConfig } = useAppStore();
  const { eventTable, eventSourceFilter } = uiConfig;

  const [sourcesFetching, fetchSources] = useSpinner();
  const [eventSources, setEventSources] = useState<string[]>([]);

  const setEventSourceFilter = async (source: string | null): Promise<void> => {
    setUiConfig({ ...uiConfig, eventSourceFilter: source });
    setUiConfig(await window.api.updateUiConfig({ eventSourceFilter: source }));
  };

  useAsyncEffect(
    async () => {
      await fetchSources(
        () => !!selectedServerId,
        async () => {
          const { success, data, error } = await window.api.getEventSources(
            selectedServerId as string,
            eventTable
          );
          if (success && data) {
            setEventSources(data);
            if (eventSourceFilter && !data.includes(eventSourceFilter)) {
              await setEventSourceFilter(null);
            }
          }
          if (error) {
            await AppToaster.error(error);
          }
        }
      );
    },
    () => {},
    [selectedServerId, eventTable, fetchSources]
  );

  return (
    <PopoverNext
      content={
        <Menu>
          <MenuItem
            text="All sources"
            icon={!eventSourceFilter ? 'tick' : 'blank'}
            onClick={() => setEventSourceFilter(null)}
          />
          <MenuDivider />
          {sourcesFetching ? (
            <MenuItem
              disabled
              text={
                <MenuSpinnerContainer>
                  <Spinner size={16} />
                </MenuSpinnerContainer>
              }
            />
          ) : eventSources.length === 0 ? (
            <MenuItem disabled text="No sources" />
          ) : (
            eventSources.map(source => (
              <MenuItem
                key={source}
                text={source}
                icon={eventSourceFilter === source ? 'tick' : 'blank'}
                onClick={() => setEventSourceFilter(source)}
              />
            ))
          )}
        </Menu>
      }
      placement="bottom-start">
      <Button icon="filter-list" text={eventSourceFilter || 'All sources'} endIcon="caret-down" />
    </PopoverNext>
  );
};

const MenuSpinnerContainer = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 10px;
`;

export default EventSourceFilter;
