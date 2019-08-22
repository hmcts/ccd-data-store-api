# Keep hub.Dockerfile aligned to this file as far as possible
ARG JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
ARG APP_INSIGHTS_AGENT_VERSION=2.3.1-SNAPSHOT

FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.0
LABEL maintainer="https://github.com/hmcts/ccd-data-store-api"

COPY build/libs/core-case-data.jar /opt/app/
COPY lib/applicationinsights-agent-2.3.1-SNAPSHOT.jar lib/AI-Agent.xml /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:4452/status/health || exit 1

EXPOSE 4452

CMD ["core-case-data.jar"]
