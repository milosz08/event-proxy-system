# Event Proxy Server

Standalone server receiving events from a fake SMTP server (on the local network), parsing them and
sending them via real-time communication channels (SSE). It also includes a simple REST client with
CRUD operations.

[GitHub repository](https://github.com/milosz08/event-proxy-system)
| [Support](https://github.com/sponsors/milosz08)

## Build image

```bash
docker build -t milosz08/event-proxy-server .
```

## Create container

* Using command:

```bash
docker run -d \
  --name event-proxy-server \
  -p 8080:8080 \
  -e EVENT_PROXY_SERVER_XMS=128m \
  -e EVENT_PROXY_SERVER_XMX=128m \
  -e HTTP_PORT=4365 \
  -e SSE_HEARTBEAT_INTERVAL_SEC=10 \
  -e SSE_HANDSHAKE_PENDING_SEC=180 \
  -e SMTP_PORT=4366 \
  -e SMTP_THREAD_POOL_SIZE=10 \
  -e SMTP_QUEUE_CAPACITY=10 \
  -e SMTP_SENDER_SUFFIX=event-proxy-system \
  -e DB_PATH=events.db \
  -e DB_POOL_SIZE=5 \
  -e SESSION_TTL_SEC=7200 \
  -e SESSION_CLEAR_INTERVAL_SEC=3600 \
  -e ACCOUNT_USERNAME=admin \
  -e ACCOUNT_PASSWORD_LENGTH=20 \
  -e ACCOUNT_PASSWORD_HASH_STRENGTH=10 \
  milosz08/event-proxy-server:latest
```

* Using `docker-compose.yml` file:

```yaml
services:
  event-proxy-server:
    container_name: event-proxy-server
    image: milosz08/event-proxy-server:latest
    ports:
      - '8080:8080'
    environment:
      EVENT_PROXY_SERVER_XMS: 128m
      EVENT_PROXY_SERVER_XMX: 128m
      # http and smtp servers
      HTTP_PORT: 4365
      SSE_HEARTBEAT_INTERVAL_SEC: 10
      SSE_HANDSHAKE_PENDING_SEC: 180
      SMTP_PORT: 4366
      SMTP_THREAD_POOL_SIZE: 10
      SMTP_QUEUE_CAPACITY: 10
      SMTP_SENDER_SUFFIX: event-proxy-system
      # database
      DB_PATH: events.db
      DB_POOL_SIZE: 5
      # session
      SESSION_TTL_SEC: 7200
      SESSION_CLEAR_INTERVAL_SEC: 3600
      # account
      ACCOUNT_USERNAME: admin
      ACCOUNT_PASSWORD_LENGTH: 20
      ACCOUNT_PASSWORD_HASH_STRENGTH: 10
    networks:
      - event-proxy-server-network

  # other containers...

networks:
  event-proxy-server-network:
    driver: bridge
```

## Author

Created by Miłosz Gilga. If you have any questions about this application, send
message: [miloszgilga@gmail.com](mailto:miloszgilga@gmail.com).

## License

This project is licensed under the Apache 2.0 License.
