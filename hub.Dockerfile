FROM gradle:jdk8 as builder

COPY . /home/gradle/src
USER root
RUN chown -R gradle:gradle /home/gradle/src
USER gradle

WORKDIR /home/gradle/src
RUN gradle assemble

FROM openjdk:8-jre-alpine

COPY --from=builder /home/gradle/src/build/libs/core-case-data.jar /opt/app/

WORKDIR /opt/app

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q http://localhost:4452/health || exit 1

EXPOSE 4452

ENTRYPOINT exec java ${JAVA_OPTS} -jar "/opt/app/core-case-data.jar"
