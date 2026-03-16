import { Button, Colors, Divider, NonIdealState, Spinner } from '@blueprintjs/core';
import { useAsyncEffect } from '@reactuses/core';
import useEventUpdatableActions from '@renderer/hooks/use-event-updatable-actions';
import useSpinner from '@renderer/hooks/use-spinner';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import { formatTimestamp } from '@renderer/utils/utils';
import { EventDetails as EventDetailsType } from '@shared-types/shared';
import React, { useState } from 'react';
import styled from 'styled-components';

const EventDetails: React.FC = (): React.ReactElement | null => {
  const {
    selectedEvents,
    uiConfig: { eventTable, selectedServerId },
    updateEvent,
    setSelectedEvents,
  } = useAppStore();

  const [detailsLoading, runDetailsFetch] = useSpinner();
  const { onArchive, onUnarchive, onDelete } = useEventUpdatableActions();
  const [eventDetails, setEventDetails] = useState<EventDetailsType | null>(null);

  const handleMarkAsUnread = async (eventId: number): Promise<void> => {
    if (!selectedServerId) {
      return;
    }
    const { success, error } = await window.api.markEventAsUnread(
      selectedServerId,
      eventTable,
      eventId
    );
    if (success) {
      setSelectedEvents([]);
      updateEvent(eventId, { isUnread: true });
    }
    if (error) {
      await AppToaster.error(error);
    }
  };

  useAsyncEffect(
    async () => {
      if (selectedEvents.length !== 1) {
        setEventDetails(null);
      }
      await runDetailsFetch(
        () => !!(selectedServerId && selectedEvents.length === 1),
        async () => {
          const { success, error, data } = await window.api.getEventDetails(
            selectedServerId as string,
            eventTable,
            selectedEvents[0]
          );
          if (success && data) {
            setEventDetails(data);
          }
          if (error) {
            await AppToaster.error(error);
          }
        }
      );
    },
    () => {},
    [selectedServerId, selectedEvents, eventTable]
  );

  if (detailsLoading) {
    return (
      <EventDetailsNonIdealStateContainer>
        <Spinner size={20} />
      </EventDetailsNonIdealStateContainer>
    );
  }

  if (!eventDetails) {
    return (
      <EventDetailsNonIdealStateContainer>
        <NonIdealState
          description="Select an event from the list to view its details."
          icon="form"
          iconSize={32}
          title="No single event selected"
        />
      </EventDetailsNonIdealStateContainer>
    );
  }

  return (
    <EventDetailsContainer>
      <TitleBarContainer>
        <TitleContainer>
          <TitleHeader>
            ({eventDetails.id}) {eventDetails.subject}
          </TitleHeader>
          <TitleParagraph>
            From: {eventDetails.eventSource}, {formatTimestamp(eventDetails.eventTime)}
          </TitleParagraph>
        </TitleContainer>
        <LeftButtonsContainer>
          <Button
            text="Mark as unread"
            disabled={eventDetails.isUnread}
            onClick={() => handleMarkAsUnread(eventDetails.id)}
          />
          {eventTable === 'EVENTS' && (
            <Button
              icon="archive"
              title="Archive event"
              intent="warning"
              variant="outlined"
              onClick={async () => await onArchive()}
            />
          )}
          {eventTable === 'EVENTS_ARCHIVE' && (
            <Button
              icon="unarchive"
              title="Unarchive event"
              intent="warning"
              variant="outlined"
              onClick={async () => await onUnarchive()}
            />
          )}
          <Button
            icon="trash"
            title={`Delete ${eventTable === 'EVENTS_ARCHIVE' ? 'forever ' : ''}event`}
            intent="danger"
            variant={eventTable === 'EVENTS' ? 'outlined' : 'solid'}
            onClick={async () => await onDelete()}
          />
        </LeftButtonsContainer>
      </TitleBarContainer>
      <Divider compact />
      <RawBodyContainer dangerouslySetInnerHTML={{ __html: eventDetails.rawBody }} />
    </EventDetailsContainer>
  );
};

const EventDetailsNonIdealStateContainer = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
`;

const EventDetailsContainer = styled.div`
  display: flex;
  flex-direction: column;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  overflow: hidden;
`;

const TitleBarContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 10px;
  flex-shrink: 0;
  padding: 10px;
`;

const TitleContainer = styled.div`
  display: block;
`;

const TitleHeader = styled.h1`
  font-size: 1rem;
  margin: 0;
  font-weight: 500;
`;

const TitleParagraph = styled.p`
  margin: 0;
  font-size: 0.8rem;
  color: ${Colors.GRAY4};
`;

const LeftButtonsContainer = styled.div`
  display: flex;
  gap: 10px;
  flex-shrink: 0;
`;

const RawBodyContainer = styled.div`
  padding: 10px;
  white-space: pre-wrap;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  font-family: monospace;
`;

export default EventDetails;
