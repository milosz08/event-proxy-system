import type { AxiosInstance, AxiosResponse } from 'axios';
import axios from 'axios';
import type { ApiResult } from '../../@types/shared';
import { createScopedLogger } from '../logger';
import { CryptoService, EncryptedRestMessage } from '../service/crypto-service';
import store, { ServerConfig } from '../store';
import { extractErrorMessage } from '../utils';

export type StreamHandlers<T> = {
  onData: (encryptedData: T) => void;
  onError: (error: string) => void;
};

const logger = createScopedLogger('request');

export async function safeRequest<T>(
  requestFn: () => Promise<AxiosResponse<T>>,
  serverName: string = 'localhost',
  contextName: string = 'API'
): Promise<ApiResult<T>> {
  const start = performance.now();
  try {
    const response = await requestFn();
    return {
      success: true,
      data: response.data,
      resTimeMillis: performance.now() - start,
    };
  } catch (err) {
    const errorMsg = extractErrorMessage(err);
    logger.error(serverName, `${contextName} error`, errorMsg);
    return {
      success: false,
      error: errorMsg,
      resTimeMillis: performance.now() - start,
    };
  }
}

export async function safeEncryptedRequest<T>(
  requestFn: () => Promise<AxiosResponse<EncryptedRestMessage>>,
  cryptoService: CryptoService,
  server: ServerConfig,
  contextName: string = 'API'
): Promise<ApiResult<T>> {
  const { privateKey } = store.get('rsaKeys');
  const { data, error, ...apiRest } = await safeRequest(requestFn, server.name, contextName);
  if (!data) {
    return { success: false, error };
  }
  const { aes, ...encryptedMessage } = data;
  const { data: sessionKey, error: encryptionError } = cryptoService.decryptSessionKey(
    server,
    aes,
    privateKey
  );
  if (!sessionKey) {
    return { success: false, error: encryptionError };
  }
  const { data: decryptedData, error: decryptionError } = cryptoService.decryptPayload<T>(
    server,
    encryptedMessage,
    sessionKey
  );
  return { ...apiRest, success: !decryptionError, error: decryptionError, data: decryptedData };
}

export async function safeStreamRequest<T>(
  axiosInstance: AxiosInstance,
  urlPath: string,
  handlers: StreamHandlers<T>,
  serverName: string = 'localhost',
  contextName: string = 'STREAM_API'
): Promise<() => void> {
  const controller = new AbortController();
  const start = performance.now();
  try {
    const response = await axiosInstance.get(urlPath, {
      responseType: 'stream',
      signal: controller.signal,
    });
    const stream = response.data;
    let buffer = '';
    stream.on('data', (chunk: Buffer) => {
      buffer += chunk.toString();
      const parts = buffer.split('\n\n');
      buffer = parts.pop() || '';
      parts.forEach(part => parseSSEMessage<T>(part, handlers.onData, contextName, serverName));
    });
    stream.on('error', (err: unknown) => {
      if (axios.isCancel(err)) {
        return;
      }
      const msg = extractErrorMessage(err);
      logger.error(serverName, `${contextName} stream error`, msg);
      handlers.onError(msg);
    });
    stream.on('end', () => {
      const elapsed = (performance.now() - start).toFixed(0);
      logger.info(serverName, `${contextName} stream ended after ${elapsed}ms`);
    });
  } catch (err) {
    const errorMsg = extractErrorMessage(err);
    logger.error(serverName, `${contextName} connection failed`, errorMsg);
    handlers.onError(errorMsg);
  }
  return () => {
    logger.info(serverName, `${contextName} aborting connection`);
    controller.abort();
  };
}

function parseSSEMessage<T>(
  rawString: string,
  onData: (data: T) => void,
  context: string,
  serverName: string
): void {
  const lines = rawString.split('\n');
  const dataLine = lines.find(line => line.startsWith('data:'));
  if (!dataLine) {
    return;
  }
  const jsonStr = dataLine.substring(5).trim();
  if (!jsonStr) {
    return;
  }
  try {
    const parsed = JSON.parse(jsonStr) as T;
    onData(parsed);
  } catch (err) {
    const errorMsg = extractErrorMessage(err);
    logger.error(serverName, `${context} failed to parse json`, errorMsg);
  }
}
