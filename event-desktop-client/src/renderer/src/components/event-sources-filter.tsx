import { Button, Menu, MenuDivider, MenuItem, PopoverNext, Spinner } from '@blueprintjs/core';
import { useAsyncEffect } from '@reactuses/core';
import useSpinner from '@renderer/hooks/use-spinner';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useMemo, useState } from 'react';
import styled from 'styled-components';

const EventSourceFilter: React.FC = () => {
  const { events, uiConfig, setUiConfig } = useAppStore();
  const { eventTable, selectedServerId, eventSourceFilter } = uiConfig;

  const [sourcesFetching, fetchSources] = useSpinner();
  const [apiSources, setApiSources] = useState<string[]>([]);

  const setEventSourceFilter = async (source: string | null): Promise<void> => {
    setUiConfig({ ...uiConfig, eventSourceFilter: source });
    setUiConfig(await window.api.updateUiConfig({ eventSourceFilter: source }));
  };

  const eventSources = useMemo(() => {
    const activeSources = events.map(e => e.eventSource);
    const combinedSources = new Set([...apiSources, ...activeSources]);
    return Array.from(combinedSources).sort();
  }, [events, apiSources]);

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
            setApiSources(data);
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

  useAsyncEffect(
    async () => {
      if (
        eventSourceFilter &&
        eventSources.length > 0 &&
        !eventSources.includes(eventSourceFilter)
      ) {
        await setEventSourceFilter(null);
      }
    },
    () => {},
    [eventSources, eventSourceFilter]
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
