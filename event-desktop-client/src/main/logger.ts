import winston from 'winston';
import { app } from 'electron';
import {join} from 'node:path';

const logDir = join(app.getPath('userData'), 'logs');

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.json(),
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
  logger.add(new winston.transports.Console({
    format: winston.format.simple(),
  }));
}

export { logger };
