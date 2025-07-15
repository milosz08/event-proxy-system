# Event Proxy System

[[Docker image](https://hub.docker.com/r/milosz08/event-proxy-server)] |
[[About project](https://miloszgilga.pl/project/event-proxy-system)]

Event proxy system receiving events from a fake SMTP server (on the local network), parsing them and
sending them via real-time communication channels (SSE). It also includes a simple REST client with
CRUD operations and desktop application receiving these events from multiple proxy servers.

## Table of content
* [Basic concept](#basic-concept)
* [Clone, install and run](#clone-install-and-run)
* [Disclaimer](#disclaimer)
* [Author](#author)
* [License](#license)

## Basic concept

The core concept involves a fully isolated local network (e.g., 192.168.X.X) containing a mock SMTP
proxy server that receives events from internal devices, such as a DVR. Data from this server is
stored in a database and delivered to clients via a secure Cloudflare tunnel, which removes the need
for risky port forwarding. Furthermore, messages are End-to-End encrypted, ensuring that no one, not
even Cloudflare's edge servers, can decrypt the content.

The server does not contain complex and heavyweight frameworks (like Spring, Spring Boot) and is
built on a pure Jetty server to achieve the smallest possible memory footprint. For the same reason,
the decision was made to use good old Swing for the client, instead of the heavyweight Electron.

![](.github/flow-diagram.svg)

## Clone, install and run

1. Clone repository on your local machine via:

```bash
$ git clone https://github.com/milosz08/event-proxy-system
```

2. Build client and server (or build in separately)

```bash
$ ./mvnw clean package # build all
$ ./mvnw clean package -pl event-proxy-server # build server
$ ./mvnw clean package -pl event-client # build client
```

3. Run server in your local network area (on Raspberry Pi or other device with JVM 17):

```bash
java -Xms=128m -Xmx=128m -jar event-proxy-server.jar
```

4. Alternatively, get server from DockerHub repository and run it using `docker-compose.yml` file
   (check [docker README](/docker/README.md)).

5. Run client via (make sure you have at least JVM 17 on your machine):

```bash
java -Xms=512m -Xmx=512m -jar event-client.jar
```

Client can work on Windows, Linux (with GUI) and macOS.

## Disclaimer

This software is provided "as is", without any warranty. You use it at your own risk, and the author
is not responsible for any damages resulting from its use.

## Author

Created by Miłosz Gilga. If you have any questions about this software, send
message: [miloszgilga@gmail.com](mailto:miloszgilga@gmail.com).

## License

This project is licensed under the Apache 2.0 License.
