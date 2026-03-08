import { Checkbox, Colors, NonIdealState, Spinner } from '@blueprintjs/core';
import { useAsyncEffect } from '@reactuses/core';
import useEventUpdatableActions from '@renderer/hooks/use-event-updatable-actions';
import useTableSelection, { SelectionModifiers } from '@renderer/hooks/use-table-selection';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import { getEventSourceColors } from '@renderer/utils/color';
import { formatTimestamp } from '@renderer/utils/utils';
import { EventPayload } from '@shared-types/shared';
import React, { useCallback, useEffect } from 'react';
import { List, RowComponentProps, useListRef } from 'react-window';
import { useInfiniteLoader } from 'react-window-infinite-loader';
import styled from 'styled-components';

type RowProps = {
  events: EventPayload[];
  isSelected: (id: number) => boolean;
  handleRowClick: (index: number, modifiers: SelectionModifiers) => void;
  isRowLoaded: (index: number) => boolean;
  markAsRead: (index: number) => void;
};

type Props = {
  rowHeight?: number;
  batchSize?: number;
};

const EventsTable: React.FC<Props> = ({ rowHeight = 30, batchSize = 50 }): React.ReactElement => {
  const {
    selectedServerId,
    uiConfig: { sortByAscending, eventTable, eventSourceFilter },
    searchValue,
    events,
    hasMoreEvents,
    loadedHistoricalRecords,
    setEvents,
    appendEvents,
    setHasMoreEvents,
    updateEvent,
  } = useAppStore();

  const listRef = useListRef(null);
  const eventUpdatableActions = useEventUpdatableActions();
  const {
    isAllSelected,
    isIndeterminate,
    lastSelectedIndex,
    handleRowClick,
    handleCombinedKeyDown,
    handleKeyUpHotkeys,
    handleSelectAll,
    clearSelection,
    isSelected,
  } = useTableSelection(eventUpdatableActions);

  const loadMoreRows = useCallback(async () => {
    if (!selectedServerId || !hasMoreEvents) {
      return;
    }
    const { success, error, data } = await window.api.getPageableEvents(
      selectedServerId as string,
      eventTable,
      searchValue,
      sortByAscending,
      loadedHistoricalRecords,
      batchSize,
      eventSourceFilter
    );
    if (success) {
      appendEvents(data ? data.elements : []);
      if (data) {
        setHasMoreEvents(data.hasNext);
      }
    }
    if (error) {
      await AppToaster.error(error);
    }
  }, [
    appendEvents,
    setHasMoreEvents,
    selectedServerId,
    hasMoreEvents,
    eventTable,
    searchValue,
    sortByAscending,
    loadedHistoricalRecords,
    batchSize,
    eventSourceFilter,
  ]);

  const markAsRead = useCallback(
    async (index: number) => {
      const row = events[index];
      if (!row || !row.isUnread || !selectedServerId) {
        return;
      }
      updateEvent(row.id, { isUnread: false });
      const { error } = await window.api.markEventAsRead(selectedServerId, eventTable, row.id);
      if (error) {
        await AppToaster.error(error);
      }
    },
    [eventTable, events, selectedServerId, updateEvent]
  );

  const rowCount = hasMoreEvents ? events.length + 1 : events.length;

  const isRowLoaded = useCallback(
    (index: number) => !hasMoreEvents || index < events.length,
    [hasMoreEvents, events.length]
  );

  const onRowsRendered = useInfiniteLoader({
    isRowLoaded,
    loadMoreRows,
    rowCount,
    minimumBatchSize: batchSize,
  });

  const RenderRow = useCallback(
    ({
      index,
      style,
      events,
      isSelected,
      handleRowClick,
      ariaAttributes,
      markAsRead,
    }: RowComponentProps<RowProps>) => {
      if (!isRowLoaded(index) || !events[index]) {
        return (
          <LoadingRow style={style} {...ariaAttributes}>
            <Spinner size={16} />
          </LoadingRow>
        );
      }
      const row = events[index];
      const selected = isSelected(row.id);
      const { base: rowColor, hover: rowColorHover } = getEventSourceColors(row.eventSource);

      const onRowInteraction = (e: React.MouseEvent): void => {
        handleRowClick(index, { ctrlKey: e.ctrlKey, shiftKey: e.shiftKey });
        markAsRead(index);
      };
      return (
        <SelectableRow
          {...ariaAttributes}
          style={style}
          $isSelected={selected}
          $isUnread={row.isUnread}
          $eventColor={rowColor}
          $eventColorHover={rowColorHover}
          onClick={onRowInteraction}>
          <CheckboxCell onClick={e => e.stopPropagation()}>
            <StyledCheckbox
              checked={selected}
              onChange={() => {
                handleRowClick(index, {
                  ctrlKey: true,
                  shiftKey: false,
                });
              }}
            />
          </CheckboxCell>
          <Cell>
            ({row.id}) {row.subject}
          </Cell>
          <Cell>{row.eventSource}</Cell>
          <Cell>{formatTimestamp(row.eventTime)}</Cell>
        </SelectableRow>
      );
    },
    [isRowLoaded]
  );

  useEffect(() => {
    setEvents([]);
    setHasMoreEvents(true);
    clearSelection();
    if (listRef.current) {
      listRef.current.scrollToRow({ index: 0, align: 'start' });
    }
  }, [
    setEvents,
    setHasMoreEvents,
    clearSelection,
    listRef,
    eventTable,
    selectedServerId,
    sortByAscending,
    searchValue,
    eventSourceFilter,
  ]);

  useAsyncEffect(
    async () => {
      if (lastSelectedIndex !== null && listRef.current) {
        listRef.current.scrollToRow({
          index: lastSelectedIndex,
          align: 'auto',
        });
        await markAsRead(lastSelectedIndex);
      }
    },
    () => {},
    [lastSelectedIndex, listRef]
  );

  if (isRowLoaded(0) && events.length === 0) {
    return (
      <NonIdealState
        description="There are currently no events to display for this server."
        icon="search"
        iconSize={32}
        title="No events found"
      />
    );
  }

  return (
    <EventTableContainer>
      <GridHeader $rowHeight={rowHeight}>
        <CheckboxCell>
          <StyledCheckbox
            checked={isAllSelected}
            indeterminate={isIndeterminate}
            onChange={handleSelectAll}
          />
        </CheckboxCell>
        <Cell>Subject</Cell>
        <Cell>Event source</Cell>
        <Cell>Event time</Cell>
      </GridHeader>
      <ListContainer tabIndex={0} onKeyDown={handleCombinedKeyDown} onKeyUp={handleKeyUpHotkeys}>
        <List<RowProps>
          listRef={listRef}
          onRowsRendered={onRowsRendered}
          rowCount={rowCount}
          rowHeight={rowHeight}
          rowComponent={RenderRow}
          overscanCount={5}
          rowProps={{
            events,
            isSelected,
            handleRowClick,
            isRowLoaded,
            markAsRead,
          }}
        />
      </ListContainer>
    </EventTableContainer>
  );
};

const EventTableContainer = styled.div`
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
`;

const GridRow = styled.div`
  display: grid;
  grid-template-columns: 40px 1fr 200px 200px;
  align-items: center;
`;

const GridHeader = styled(GridRow)<{ $rowHeight: number }>`
  font-weight: 600;
  height: ${({ $rowHeight }) => $rowHeight}px;
  border-bottom: 1px solid ${Colors.DARK_GRAY5};
  flex-shrink: 0;
  padding-right: 15px;
`;

const SelectableRow = styled(GridRow)<{
  $isSelected: boolean;
  $isUnread?: boolean;
  $eventColor: string;
  $eventColorHover: string;
}>`
  background-color: ${({ $isSelected, $eventColor }) =>
    $isSelected ? 'rgba(113,140,154,0.5)' : $eventColor};
  font-weight: ${({ $isUnread }) => ($isUnread ? 'bold' : 'normal')};
  color: ${({ $isUnread, $isSelected }) =>
    $isSelected ? Colors.WHITE : $isUnread ? Colors.WHITE : Colors.LIGHT_GRAY3};
  cursor: pointer;
  user-select: none;

  &:hover {
    background-color: ${({ $isSelected, $eventColorHover }) =>
      $isSelected ? 'rgba(92,114,126,0.5)' : $eventColorHover};
  }
`;

const LoadingRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${Colors.GRAY3};
  box-sizing: border-box;
  padding: 40px 0;
`;

const Cell = styled.div`
  padding: 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const CheckboxCell = styled(Cell)`
  display: flex;
  justify-content: center;
`;

const StyledCheckbox = styled(Checkbox)`
  margin: 0;
`;

const ListContainer = styled.div`
  flex: 1;
  min-height: 0;
  outline: none;

  > div {
    height: 100%;
    width: 100%;
    scrollbar-gutter: stable;
    outline: none;
  }
`;

export default EventsTable;
