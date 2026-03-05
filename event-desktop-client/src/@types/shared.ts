export type UiConfig = {
  sideBySideLook: boolean;
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

export type LoginResult = {
  hasDefaultPassword?: boolean;
} & ResponseResult;

export type ServerConfigDTO = {
  id: string;
  hasDefaultPassword?: boolean;
  unreadNotifications: number;
  lastHeartbeatTimestamp?: number; // as timestamp
  lastHeartbeatStatus?: boolean;
  lastHeartbeatResTimeMillis?: number;
} & Omit<ServerInput, 'password'>;

export type ApiResult<T> = {
  data?: T;
} & ResponseResult;

export type SseEventPayload = {
  id: number;
  subject: string;
  eventSource: string;
  eventTime: string;
};
