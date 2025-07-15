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
  -e EVENT_PROXY_SERVER_XMS=64m \
  -e EVENT_PROXY_SERVER_XMX=64m \
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
      EVENT_PROXY_SERVER_XMS: 64m
      EVENT_PROXY_SERVER_XMX: 64m
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
