# Keep hub.Dockerfile aligned to this file as far as possible
FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-1.0
LABEL maintainer="https://github.com/hmcts/ccd-data-store-api"

ENV JAVA_OPTS "-Dspring.config.location=/application.properties -Djava.security.egd=file:/dev/./urandom"

COPY build/libs/core-case-data.jar /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:4452/status/health

EXPOSE 4452

CMD ["core-case-data.jar"]
