import type { AxiosResponse } from 'axios';
import axios from 'axios';
import { ResponseResult } from '../@types/shared';
import { logger } from './logger';

export type ApiResult<T> = {
  data?: T;
} & ResponseResult;

export async function safeRequest<T>(
  requestFn: () => Promise<AxiosResponse<T>>,
  serverName: string = 'localhost',
  contextName: string = 'API'
): Promise<ApiResult<T>> {
  try {
    const response = await requestFn();
    return {
      success: true,
      data: response.data,
    };
  } catch (err) {
    const errorMsg = extractErrorMessage(err);
    logger.error(`[${serverName}] [${contextName}] error:`, errorMsg);
    return {
      success: false,
      error: errorMsg,
    };
  }
}

export const extractErrorMessage = (err: unknown): string => {
  if (axios.isAxiosError(err)) {
    let msg = err.message;
    if (err.response) {
      msg += ` (status: ${err.response.status})`;
    }
    return msg || `Unknown error: ${err.code}`;
  }
  if (err instanceof Error) {
    return err.message;
  }
  return String(err);
};
