import type { ApiResult } from '../@types/shared';
import type { ConfigService } from './config-service';
import type { CryptoService, EncryptedRestMessage } from './crypto-service';
import { createScopedLogger } from './logger';
import type { NetworkSessionManager } from './network-session-manager';
import { safeEncryptedRequest } from './request';

export class EventSourceService {
  private logger = createScopedLogger(this.constructor.name);

  constructor(
    private configService: ConfigService,
    private networkManager: NetworkSessionManager,
    private cryptoService: CryptoService
  ) {}

  public async getEventSources(serverId: string): Promise<ApiResult<string[]>> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return { success: false };
    }
    const client = this.networkManager.getAxiosForServer(server);
    const url = '/api/event/source/all';
    const result = await safeEncryptedRequest<string[]>(
      () => client.get<EncryptedRestMessage>(url),
      this.cryptoService,
      server,
      url
    );
    const { error } = result;
    if (error) {
      this.logger.error(server.name, 'unable to fetch event sources', error);
    }
    return result;
  }
}
