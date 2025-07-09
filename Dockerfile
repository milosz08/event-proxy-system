FROM eclipse-temurin:17-jdk-alpine AS build

ENV BUILD_DIR=/build/event-proxy-server

RUN mkdir -p $BUILD_DIR
WORKDIR $BUILD_DIR

# copy only maven-based resources for optimized caching
COPY .mvn $BUILD_DIR/.mvn
COPY mvnw $BUILD_DIR/mvnw
COPY pom.xml $BUILD_DIR/pom.xml
COPY event-proxy-server/pom.xml $BUILD_DIR/event-proxy-server/pom.xml

RUN chmod +x $BUILD_DIR/mvnw
RUN cd $BUILD_DIR

RUN ./mvnw dependency:tree -pl event-proxy-server

# copy rest of resources
COPY event-proxy-server $BUILD_DIR/event-proxy-server
COPY docker $BUILD_DIR/docker

RUN ./mvnw clean
RUN ./mvnw package -pl event-proxy-server

FROM eclipse-temurin:17-jre-alpine

ENV BUILD_DIR=/build/event-proxy-server
ENV ENTRY_DIR=/app/event-proxy-server
ENV JAR_NAME=event-proxy-server.jar

WORKDIR $ENTRY_DIR

COPY --from=build $BUILD_DIR/.bin/$JAR_NAME $ENTRY_DIR/$JAR_NAME
COPY --from=build $BUILD_DIR/docker/entrypoint $ENTRY_DIR/entrypoint

RUN sed -i \
  -e "s/\$JAR_NAME/$JAR_NAME/g" \
  entrypoint

RUN chmod +x entrypoint

LABEL maintainer="Miłosz Gilga <miloszgilga@gmail.com>"

EXPOSE 8080
ENTRYPOINT [ "./entrypoint" ]
