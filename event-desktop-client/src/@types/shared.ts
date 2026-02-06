export type ServerInput = {
  name: string;
  url: string;
  username: string;
  password: string;
};

export type ResponseResult = {
  success: boolean;
  error?: string;
};

export type LoginResult = {
  hasDefaultPassword?: boolean;
} & ResponseResult;

export type ServerConfigDTO = {
  id: string;
  name: string;
  url: string;
  username: string;
  hasDefaultPassword?: boolean;
  unreadNotifications: number;
};
