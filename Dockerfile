ARG JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
ARG APP_INSIGHTS_AGENT_VERSION=2.6.1

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4
LABEL maintainer="https://github.com/hmcts/ccd-data-store-api"

COPY build/libs/core-case-data.jar /opt/app/
COPY lib/AI-Agent.xml /opt/app/

EXPOSE 4452

CMD [ \
    "--add-modules", "java.se", \
    "--add-exports", "java.base/jdk.internal.ref=ALL-UNNAMED", \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "--add-opens", "java.base/java.nio=ALL-UNNAMED", \
    "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", \
    "--add-opens", "java.management/sun.management=ALL-UNNAMED", \
    "--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED", \
    "core-case-data.jar" \
    ]
