ARG JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
ARG APP_INSIGHTS_AGENT_VERSION=2.5.0

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4
LABEL maintainer="https://github.com/hmcts/ccd-data-store-api"

COPY build/libs/core-case-data.jar /opt/app/
COPY lib/AI-Agent.xml /opt/app/

EXPOSE 4452

CMD ["core-case-data.jar"]
