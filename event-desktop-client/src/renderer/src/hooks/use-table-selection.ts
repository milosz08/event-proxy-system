import { HotkeyConfig, useHotkeys } from '@blueprintjs/core';
import { useAppStore } from '@renderer/store/use-app-store';
import React, { useCallback, useLayoutEffect, useMemo, useRef, useState } from 'react';

export type SelectionModifiers = {
  ctrlKey?: boolean;
  metaKey?: boolean;
  shiftKey?: boolean;
};

type Props = {
  onArchive: (ids: number[]) => Promise<void>;
  onUnarchive: (ids: number[]) => Promise<void>;
  onDelete: (ids: number[]) => Promise<void>;
};

type ReturnProps = {
  isAllSelected: boolean;
  isIndeterminate: boolean;
  lastSelectedIndex: number | null;
  handleRowClick: (index: number, modifiers: SelectionModifiers) => void;
  handleCombinedKeyDown: (event: React.KeyboardEvent<HTMLDivElement>) => void;
  handleKeyUpHotkeys: (event: React.KeyboardEvent<HTMLDivElement>) => void;
  clearSelection: () => void;
  handleSelectAll: () => void;
  isSelected: (id: number) => boolean;
};

const useTableSelection = (props: Props): ReturnProps => {
  const {
    events,
    selectedEvents,
    setSelectedEvents,
    uiConfig: { eventTable },
  } = useAppStore();
  const handlersRef = useRef(props);

  const [lastSelectedIndex, setLastSelectedIndex] = useState<number | null>(null);
  const [anchorIndex, setAnchorIndex] = useState<number | null>(null);

  const selectedIds = useMemo(() => new Set(selectedEvents), [selectedEvents]);

  useLayoutEffect(() => {
    handlersRef.current = props;
  });

  const clearSelection = useCallback(() => {
    setSelectedEvents([]);
    setLastSelectedIndex(null);
    setAnchorIndex(null);
  }, [setSelectedEvents]);

  const shiftSelectionAfterAction = useCallback(() => {
    if (events.length === 0 || selectedEvents.length === 0) {
      return;
    }
    const selectedIndices = selectedEvents
      .map(id => events.findIndex(e => e.id === id))
      .filter(idx => idx !== -1)
      .sort((a, b) => a - b);

    if (selectedIndices.length === 0) {
      return;
    }
    const topMostIndex = selectedIndices[0];
    const bottomMostIndex = selectedIndices[selectedIndices.length - 1];

    let nextSelectedIndex = -1;
    let newCalculatedIndex = 0;

    if (bottomMostIndex + 1 < events.length) {
      nextSelectedIndex = bottomMostIndex + 1;
      newCalculatedIndex = Math.max(0, nextSelectedIndex - selectedIndices.length);
    } else if (topMostIndex > 0) {
      nextSelectedIndex = topMostIndex - 1;
      newCalculatedIndex = nextSelectedIndex;
    }
    if (nextSelectedIndex !== -1 && events[nextSelectedIndex]) {
      const nextId = events[nextSelectedIndex].id;
      setSelectedEvents([nextId]);
      setLastSelectedIndex(newCalculatedIndex);
      setAnchorIndex(newCalculatedIndex);
    } else {
      clearSelection();
    }
  }, [events, selectedEvents, setSelectedEvents, clearSelection]);

  const handleArchiveEvents = useCallback(
    async (e?: KeyboardEvent) => {
      if (selectedEvents.length === 0 || e?.repeat) {
        return;
      }
      const idsToProcess = Array.from(selectedEvents);
      shiftSelectionAfterAction();
      await handlersRef.current.onArchive(idsToProcess);
    },
    [selectedEvents, shiftSelectionAfterAction]
  );

  const handleUnarchiveEvents = useCallback(
    async (e?: KeyboardEvent) => {
      if (selectedEvents.length === 0 || e?.repeat) {
        return;
      }
      const idsToProcess = Array.from(selectedEvents);
      shiftSelectionAfterAction();
      await handlersRef.current.onUnarchive(idsToProcess);
    },
    [selectedEvents, shiftSelectionAfterAction]
  );

  const handleHardDeleteEvents = useCallback(
    async (e?: KeyboardEvent) => {
      if (selectedEvents.length === 0 || e?.repeat) {
        return;
      }
      const idsToProcess = Array.from(selectedEvents);
      shiftSelectionAfterAction();
      await handlersRef.current.onDelete(idsToProcess);
    },
    [selectedEvents, shiftSelectionAfterAction]
  );

  const hotkeys = useMemo<HotkeyConfig[]>(
    () => [
      {
        combo: 'del',
        global: false,
        group: 'Table actions',
        label: `${eventTable === 'EVENTS' ? 'Archive' : 'Hard delete'} selected events`,
        preventDefault: true,
        onKeyDown: eventTable === 'EVENTS' ? handleArchiveEvents : handleHardDeleteEvents,
      },
      {
        combo: 'shift + del',
        global: false,
        group: 'Table actions',
        label: `${eventTable === 'EVENTS' ? 'Unarchive' : 'Hard delete'} selected events`,
        preventDefault: true,
        onKeyDown: eventTable === 'EVENTS' ? handleHardDeleteEvents : handleUnarchiveEvents,
      },
    ],
    [eventTable, handleArchiveEvents, handleHardDeleteEvents, handleUnarchiveEvents]
  );

  const { handleKeyDown: handleKeyDownHotkeys, handleKeyUp: handleKeyUpHotkeys } =
    useHotkeys(hotkeys);

  const handleRowClick = useCallback(
    (index: number, modifiers: SelectionModifiers) => {
      if (index < 0 || index >= events.length) {
        return;
      }
      const clickedId = events[index].id;
      const newSelection = new Set(selectedEvents);

      if (modifiers.shiftKey && anchorIndex !== null) {
        const start = Math.min(anchorIndex, index);
        const end = Math.max(anchorIndex, index);

        if (!modifiers.ctrlKey && !modifiers.metaKey) {
          newSelection.clear();
        }
        for (let i = start; i <= end; i++) {
          newSelection.add(events[i].id);
        }
        setLastSelectedIndex(index);
      } else if (modifiers.ctrlKey || modifiers.metaKey) {
        if (newSelection.has(clickedId)) {
          newSelection.delete(clickedId);
        } else {
          newSelection.add(clickedId);
        }
        setAnchorIndex(index);
        setLastSelectedIndex(index);
      } else {
        if (selectedEvents.length === 1 && selectedEvents[0] === clickedId) {
          newSelection.clear();
          setAnchorIndex(null);
          setLastSelectedIndex(null);
        } else {
          newSelection.clear();
          newSelection.add(clickedId);
          setAnchorIndex(index);
          setLastSelectedIndex(index);
        }
      }
      setSelectedEvents(Array.from(newSelection));
    },
    [events, selectedEvents, anchorIndex, setSelectedEvents]
  );

  const handleKeyDownSelection = useCallback(
    (event: React.KeyboardEvent) => {
      if (events.length === 0) {
        return;
      }
      const modifiers = {
        shiftKey: event.shiftKey,
        ctrlKey: false,
        metaKey: false,
      };
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        if (lastSelectedIndex === events.length - 1) {
          return;
        }
        handleRowClick(
          lastSelectedIndex !== null ? Math.min(lastSelectedIndex + 1, events.length - 1) : 0,
          modifiers
        );
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        if (lastSelectedIndex === 0) {
          return;
        }
        handleRowClick(
          lastSelectedIndex !== null ? Math.max(lastSelectedIndex - 1, 0) : 0,
          modifiers
        );
      }
    },
    [events.length, lastSelectedIndex, handleRowClick]
  );

  const handleCombinedKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLDivElement>) => {
      handleKeyDownSelection(e);
      handleKeyDownHotkeys(e);
    },
    [handleKeyDownSelection, handleKeyDownHotkeys]
  );

  const isAllSelected = events.length > 0 && selectedEvents.length === events.length;
  const isIndeterminate = selectedEvents.length > 0 && selectedEvents.length < events.length;

  const handleSelectAll = useCallback(() => {
    if (events.length === 0) {
      return;
    }
    if (isAllSelected) {
      setSelectedEvents([]);
      setLastSelectedIndex(null);
      setAnchorIndex(null);
    } else {
      setSelectedEvents(events.map(event => event.id));
      setLastSelectedIndex(null);
      setAnchorIndex(null);
    }
  }, [events, isAllSelected, setSelectedEvents]);

  return {
    isAllSelected,
    isIndeterminate,
    lastSelectedIndex,
    handleRowClick,
    handleCombinedKeyDown,
    handleKeyUpHotkeys,
    clearSelection,
    handleSelectAll,
    isSelected: (id: number) => selectedIds.has(id),
  };
};

export default useTableSelection;
