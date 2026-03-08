import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import { ApiResult } from '@shared-types/shared';
import { useCallback } from 'react';

type RequestPayload = {
  callbackFn: (serverId: string) => Promise<ApiResult<void>>;
  successText: string;
  onUiUpdate: () => void;
};

type ReturnProps = {
  onArchive: () => Promise<void>;
  onUnarchive: () => Promise<void>;
  onDelete: () => Promise<void>;
};

const useEventUpdatableActions = (): ReturnProps => {
  const {
    events,
    selectedServerId,
    selectedEvents,
    setEvents,
    setSelectedEvents,
    removeSelectedEvents,
    removeEvents,
    uiConfig: { eventSourceFilter, eventTable },
  } = useAppStore();

  const singleRequest = useCallback(
    async ({ callbackFn, successText, onUiUpdate }: RequestPayload) => {
      if (!selectedServerId) {
        return;
      }
      const { success, error } = await callbackFn(selectedServerId);
      if (success) {
        await AppToaster.success(successText);
        onUiUpdate();
      }
      if (error) {
        await AppToaster.error(error);
      }
    },
    [selectedServerId]
  );

  const conditionalRequest = useCallback(
    async (bulk: RequestPayload, all: RequestPayload) => {
      if (events.length === selectedEvents.length) {
        await singleRequest({ ...all });
      } else {
        await singleRequest({ ...bulk });
      }
    },
    [events.length, selectedEvents.length, singleRequest]
  );

  const onUiBulkUpdate = (): void => {
    removeSelectedEvents(selectedEvents);
    removeEvents(selectedEvents);
  };

  const onUiAllUpdate = (): void => {
    setSelectedEvents([]);
    setEvents([]);
  };

  const onArchive = async (): Promise<void> => {
    await conditionalRequest(
      {
        callbackFn: serverId => window.api.bulkArchiveEvents(serverId, selectedEvents),
        successText: `Archived ${selectedEvents.length} events`,
        onUiUpdate: onUiBulkUpdate,
      },
      {
        callbackFn: serverId => window.api.allArchiveEvents(serverId, eventSourceFilter),
        successText: 'Archived all events',
        onUiUpdate: onUiAllUpdate,
      }
    );
  };

  const onUnarchive = async (): Promise<void> => {
    await conditionalRequest(
      {
        callbackFn: serverId => window.api.bulkUnarchiveEvents(serverId, selectedEvents),
        successText: `Unarchived ${selectedEvents.length} events`,
        onUiUpdate: onUiBulkUpdate,
      },
      {
        callbackFn: serverId => window.api.allUnarchiveEvents(serverId, eventSourceFilter),
        successText: 'Unarchived all events',
        onUiUpdate: onUiAllUpdate,
      }
    );
  };

  const onDelete = async (): Promise<void> => {
    await conditionalRequest(
      {
        callbackFn: serverId => window.api.bulkDeleteEvents(serverId, eventTable, selectedEvents),
        successText: `Deleted ${selectedEvents.length} events`,
        onUiUpdate: onUiBulkUpdate,
      },
      {
        callbackFn: serverId => window.api.allDeleteEvents(serverId, eventTable, eventSourceFilter),
        successText: 'Deleted all events',
        onUiUpdate: onUiAllUpdate,
      }
    );
  };

  return {
    onArchive,
    onUnarchive,
    onDelete,
  };
};

export default useEventUpdatableActions;
