import { Button, Divider, Icon, InputGroup, SegmentedControl } from '@blueprintjs/core';
import EventSourceFilter from '@renderer/components/event-sources-filter';
import useEventUpdatableActions from '@renderer/hooks/use-event-updatable-actions';
import { useAppStore } from '@renderer/store/use-app-store';
import { EventTable, UiConfig } from '@shared-types/shared';
import React, { useMemo, useState } from 'react';
import styled from 'styled-components';

const Toolbar: React.FC = (): React.ReactElement => {
  const { uiConfig, selectedEvents, setUiConfig, setSearchValue, setSelectedEvents } =
    useAppStore();
  const { sideBySideLook, showDetails, sortByAscending, eventTable } = uiConfig;

  const [searchText, setSearchText] = useState('');
  const { onArchive, onUnarchive, onDelete } = useEventUpdatableActions();

  const toggleUiProperty = async (key: keyof UiConfig): Promise<void> => {
    const newValue = !uiConfig[key];
    setUiConfig({ ...uiConfig, [key]: newValue });
    setUiConfig(await window.api.updateUiConfig({ [key]: newValue }));
  };

  const eventsCountText = useMemo(() => {
    if (selectedEvents.length > 0) {
      return ` (${selectedEvents.length})`;
    }
    return '';
  }, [selectedEvents]);

  return (
    <ToolbarContainer>
      <Button
        icon={sideBySideLook ? 'layout-two-columns' : 'layout-two-rows'}
        title={sideBySideLook ? 'Set horizontal layout' : 'Set vertical layout'}
        disabled={!showDetails}
        onClick={() => toggleUiProperty('sideBySideLook')}
      />
      <Button
        icon={
          <DetailsButtonIcon
            icon={showDetails ? 'list-columns' : 'list-detail-view'}
            $isRotated={!sideBySideLook}
          />
        }
        title={showDetails ? 'Show details' : 'Hide details'}
        onClick={() => toggleUiProperty('showDetails')}
      />
      <Button
        icon={sortByAscending ? 'sort-asc' : 'sort-desc'}
        text="Sort by: date"
        onClick={() => toggleUiProperty('sortByAscending')}
      />
      <EventSourceFilter />
      <Divider />
      <SegmentedControl
        options={[
          { label: 'Events', value: 'EVENTS' },
          { label: 'Archive', value: 'EVENTS_ARCHIVE' },
        ]}
        value={eventTable}
        defaultValue="EVENTS"
        onValueChange={async value => {
          setSelectedEvents([]);
          setUiConfig({ ...uiConfig, eventTable: value as EventTable });
          setUiConfig(await window.api.updateUiConfig({ eventTable: value as EventTable }));
        }}
        size="small"
      />
      <StyledInputGroup
        value={searchText}
        onChange={e => setSearchText(e.target.value)}
        onKeyDown={e => {
          if (e.key === 'Enter') {
            setSearchValue(searchText);
          } else if (e.key === 'Escape') {
            setSearchText('');
            setSearchValue('');
            e.currentTarget.blur();
          }
        }}
        placeholder="Search by subject..."
        rightElement={
          <Button icon="search" variant="minimal" onClick={() => setSearchValue(searchText)} />
        }
      />
      {eventTable === 'EVENTS' && (
        <Button
          icon="archive"
          text={`Archive${eventsCountText}`}
          disabled={selectedEvents.length === 0}
          intent="warning"
          variant="outlined"
          onClick={async () => await onArchive()}
        />
      )}
      {eventTable === 'EVENTS_ARCHIVE' && (
        <Button
          icon="unarchive"
          text={`Unarchive${eventsCountText}`}
          disabled={selectedEvents.length === 0}
          intent="warning"
          variant="outlined"
          onClick={async () => await onUnarchive()}
        />
      )}
      <Button
        icon="trash"
        text={`Delete ${eventTable === 'EVENTS_ARCHIVE' ? 'forever ' : ''} ${eventsCountText}`}
        disabled={selectedEvents.length === 0}
        intent="danger"
        variant={eventTable === 'EVENTS' ? 'outlined' : 'solid'}
        onClick={async () => await onDelete()}
      />
    </ToolbarContainer>
  );
};

const ToolbarContainer = styled.div`
  display: flex;
  justify-content: space-between;
  margin: 10px;
  gap: 10px;
`;

const StyledInputGroup = styled(InputGroup)`
  flex-grow: 1;
`;

const DetailsButtonIcon = styled(Icon)<{ $isRotated: boolean }>`
  transform: ${({ $isRotated }) => ($isRotated ? 'rotate(90deg)' : 'rotate(0deg)')};
`;

export default Toolbar;
