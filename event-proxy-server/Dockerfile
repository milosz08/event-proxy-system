FROM eclipse-temurin:17-jdk-alpine AS build

ENV BUILD_DIR=/build/event-proxy-system

RUN mkdir -p $BUILD_DIR
WORKDIR $BUILD_DIR

# copy only maven-based resources for optimized caching
COPY .mvn $BUILD_DIR/.mvn
COPY mvnw $BUILD_DIR/mvnw
COPY pom.xml $BUILD_DIR/pom.xml
COPY event-proxy-server/pom.xml $BUILD_DIR/event-proxy-server/pom.xml
COPY event-desktop-client/pom.xml $BUILD_DIR/event-desktop-client/pom.xml
COPY server.properties $BUILD_DIR/server.properties

RUN chmod +x $BUILD_DIR/mvnw
RUN cd $BUILD_DIR

RUN ./mvnw dependency:tree -pl event-proxy-server

# copy rest of resources
COPY event-proxy-server/src $BUILD_DIR/event-proxy-server/src
COPY event-proxy-server/docker $BUILD_DIR/event-proxy-server/docker

RUN ./mvnw clean package -pl event-proxy-server

FROM eclipse-temurin:17-jre-alpine

ENV BUILD_DIR=/build/event-proxy-system/event-proxy-server
ENV ENTRY_DIR=/app/event-proxy-server
ENV JAR_NAME=event-proxy-server.jar

WORKDIR $ENTRY_DIR

RUN addgroup -S epgroup && \
    adduser -S epuser -G epgroup

# for read/write SQLite database
RUN chown -R epuser:epgroup $ENTRY_DIR

COPY --chown=epuser:epgroup --from=build $BUILD_DIR/.bin/$JAR_NAME $ENTRY_DIR/$JAR_NAME
COPY --from=build $BUILD_DIR/docker/entrypoint $ENTRY_DIR/entrypoint

RUN sed -i \
  -e "s/\$JAR_NAME/$JAR_NAME/g" \
  entrypoint

RUN chmod +x entrypoint && \
    chown epuser:epgroup entrypoint

LABEL maintainer="Miłosz Gilga <miloszgilga@gmail.com>"

EXPOSE 4365 4366

USER epuser

ENTRYPOINT [ "./entrypoint" ]
