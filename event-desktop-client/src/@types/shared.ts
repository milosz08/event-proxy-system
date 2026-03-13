export type UiConfig = {
  sideBySideLook: boolean;
  showDetails: boolean;
  panelSizes: number[];
  sortByAscending: boolean;
  eventTable: EventTable;
  eventSourceFilter: string | null;
  selectedServerId: string | null;
};

export type ServerInput = {
  name: string;
  url: string;
  username: string;
  password: string;
};

export type ResponseResult = {
  success: boolean;
  error?: string;
  resTimeMillis?: number;
};

export type LoginData = {
  hasDefaultPassword?: boolean;
};

export type ServerConfigDTO = {
  id: string;
  hasDefaultPassword?: boolean;
  unreadNotifications: number;
  lastHeartbeatTimestamp?: number; // as timestamp
  lastHeartbeatStatus?: boolean;
  lastHeartbeatResTimeMillis?: number;
} & Omit<ServerInput, 'password'>;

export type PageableApiResults<T> = {
  totalElements: number;
  hasNext: boolean;
  elements: T[];
};

export type ApiResult<T> = {
  data?: T;
} & ResponseResult;

export type SseEventPayload = {
  id: number;
  subject: string;
  eventSource: string;
  eventTime: string;
};

export type EventPayload = {
  isUnread: boolean;
} & SseEventPayload;

export type EventTable = 'EVENTS' | 'EVENTS_ARCHIVE';

export type EventDetails = {
  rawBody: string;
} & EventPayload;

export type UnreadEventsCount = {
  count: number;
};
