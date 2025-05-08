#FROM gradle:8.8-jdk21
#
#WORKDIR /app
#
#COPY . .
#
#COPY /docker/development/api/docker-entrypoint.sh /
#RUN chmod +x /docker-entrypoint.sh
#
#COPY /gama/gama-platform /opt/gama-platform
#RUN chmod -R 777 /opt/gama-platform
#
#CMD ["/docker-entrypoint.sh"]

FROM gradle:8.8-jdk21 AS build

WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon -i build -x test

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=build /app/build/libs/agent_simulation_worker-0.0.1-SNAPSHOT.jar /app/api.jar

COPY /docker/development/api/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

COPY /gama/gama-platform /opt/gama-platform
RUN chmod -R 777 /opt/gama-platform

ENTRYPOINT ["/docker-entrypoint.sh"]
