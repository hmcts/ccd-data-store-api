# Keep hub.Dockerfile aligned to this file as far as possible
FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-1.0
LABEL maintainer="https://github.com/hmcts/ccd-data-store-api"

ENV JAVA_OPTS "-Djava.security.egd=file:/dev/./urandom -javaagent:/opt/app/applicationinsights-agent-2.3.1.jar"

COPY build/libs/core-case-data.jar /opt/app/
COPY lib/applicationinsights-agent-2.3.1.jar lib/AI-Agent.xml /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:4452/status/health

EXPOSE 4452

CMD ["core-case-data.jar"]
