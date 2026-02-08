import { app } from 'electron';
import { join } from 'node:path';
import winston from 'winston';

const logDir = join(app.getPath('userData'), 'logs');

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    winston.format.json()
  ),
  transports: [
    new winston.transports.File({
      filename: join(logDir, 'error.log'),
      level: 'error',
    }),
    new winston.transports.File({
      filename: join(logDir, 'combined.log'),
    }),
  ],
});

if (!app.isPackaged) {
  logger.add(
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.colorize(),
        winston.format.timestamp({ format: 'HH:mm:ss' }),
        winston.format.printf(({ level, message, timestamp, className, serverName }) => {
          const classPart = className ? `[${className}]` : '';
          const serverPart = serverName ? `[${serverName}]` : '';
          return `${timestamp} ${level}: ${classPart} ${serverPart} ${message}`
            .replace(/\s+/g, ' ')
            .trim();
        })
      ),
    })
  );
}

const logEvent = (
  level: 'info' | 'error' | 'warn' | 'debug',
  className: string,
  serverName: string,
  message: string,
  meta: object = {}
): void => {
  logger.log({
    level,
    message,
    className,
    serverName,
    ...meta,
  });
};

export const createScopedLogger = (
  className: string
): {
  info: (serverName: string, message: string) => void;
  error: (serverName: string, message: string, err?: string) => void;
  warn: (serverName: string, message: string) => void;
  debug: (serverName: string, message: string) => void;
} => ({
  info: (serverName: string, message: string) => logEvent('info', className, serverName, message),
  error: (serverName: string, message: string, err?: string) =>
    logEvent('error', className, serverName, `${message}, cause: ${err}`),
  warn: (serverName: string, message: string) => logEvent('warn', className, serverName, message),
  debug: (serverName: string, message: string) => logEvent('debug', className, serverName, message),
});
