package uk.gov.hmcts.ccd.error;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class FailureDiagnosticsTest {
    @Test
    void capturesStructuredFieldsAndOriginalExceptionWithoutLosingExistingContext() {
        var previous = MDC.getCopyOfContextMap();
        Logger logger = (Logger) LoggerFactory.getLogger(FailureDiagnosticsTest.class);
        var events = new ListAppender<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        events.start();
        logger.addAppender(events);
        try {
            MDC.put("operationId", "existing-correlation");
            MDC.put("failureCode", "outer-code");
            MDC.put("httpStatus", "outer-status");
            var failure = new IllegalStateException("PRIVATE wrapper", new SQLException("PRIVATE database cause"));
            try (var ignored = FailureDiagnostics.open(failure, 503)) {
                logger.error("Request failed", failure);
            }
            assertThat(events.list).singleElement().satisfies(event -> {
                assertThat(event.getMDCPropertyMap()).containsEntry("failureCode", "DATABASE_OPERATION_FAILED")
                    .containsEntry("httpStatus", "503").containsEntry("operationId", "existing-correlation");
                assertThat(event.getThrowableProxy().getCause().getMessage()).isEqualTo("PRIVATE database cause");
                assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()).isNotEmpty();
            });
            assertThat(MDC.get("failureCode")).isEqualTo("outer-code");
            assertThat(MDC.get("httpStatus")).isEqualTo("outer-status");
            assertThat(MDC.get("operationId")).isEqualTo("existing-correlation");
        } finally {
            logger.detachAppender(events);
            events.stop();
            MDC.clear();
            if (previous != null) {
                MDC.setContextMap(previous);
            }
        }
    }

    @Test
    void retainsExplicitBusinessCodeAndRemovesOnlyItsOwnTemporaryFields() {
        var previous = MDC.getCopyOfContextMap();
        MDC.remove("failureCode");
        MDC.remove("httpStatus");
        try {
            try (var ignored = FailureDiagnostics.open(
                new IllegalArgumentException("PRIVATE"), 400, "answers-invalid")) {
                assertThat(MDC.get("failureCode")).isEqualTo("answers-invalid");
                assertThat(MDC.get("httpStatus")).isEqualTo("400");
            }
            assertThat(MDC.get("failureCode")).isNull();
            assertThat(MDC.get("httpStatus")).isNull();
        } finally {
            MDC.clear();
            if (previous != null) {
                MDC.setContextMap(previous);
            }
        }
    }
}
