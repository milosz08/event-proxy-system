# Event Proxy System

[[Docker image](https://hub.docker.com/r/milosz08/event-proxy-server)] |
[[About project](https://miloszgilga.pl/project/event-proxy-system)]

Event proxy system receiving events from a fake SMTP server (on the local network), parsing them and
sending them via real-time communication channels (SSE). It also includes a simple REST client with
CRUD operations and desktop application receiving these events from multiple proxy servers.

## Table of content
* [Basic concept](#basic-concept)
* [Author](#author)
* [License](#license)

## Basic concept

The core concept involves a fully isolated local network (e.g., 192.168.X.X) containing a mock SMTP
proxy server that receives events from internal devices, such as a DVR. Data from this server is
stored in a database and delivered to clients via a secure Cloudflare tunnel, which removes the need
for risky port forwarding. Furthermore, messages are End-to-End encrypted, ensuring that no one, not
even Cloudflare's edge servers, can decrypt the content.

![](.github/flow-diagram.svg)

## Author

Created by Miłosz Gilga. If you have any questions about this software, send
message: [miloszgilga@gmail.com](mailto:miloszgilga@gmail.com).

## License

This project is licensed under the Apache 2.0 License.
