# Keep hub.Dockerfile aligned to this file as far as possible
ARG JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"

FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0.1
LABEL maintainer="https://github.com/hmcts/ccd-data-store-api"

COPY build/libs/core-case-data.jar /opt/app/
COPY lib/AI-Agent.xml /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:4552/status/health || exit 1

EXPOSE 4452

CMD ["core-case-data.jar"]
