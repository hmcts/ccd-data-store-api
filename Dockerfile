FROM hmcts/cnp-java-base:openjdk-jre-8-alpine-1.4
LABEL maintainer="https://github.com/hmcts/ccd-data-store-api"

ENV APP core-case-data.jar
ENV APPLICATION_TOTAL_MEMORY 980M
ENV APPLICATION_SIZE_ON_DISK_IN_MB 85

ENV JAVA_OPTS "-Dspring.config.location=/application.properties -Djava.security.egd=file:/dev/./urandom"

COPY build/libs/$APP /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:4452/status/health

EXPOSE 4452
