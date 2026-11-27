package uk.gov.hmcts.ccd.endpoint.exceptions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorBoundaryContractTest {
    @Test
    void realFieldValidationRetainsBusinessMessageAndLegacyShape() throws Exception {
        var mvc = standaloneSetup(new Probe())
            .setControllerAdvice(new RestExceptionHandler(mock(AppInsights.class), false)).build();
        mvc.perform(get("/validation-probe"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Field Names Invalid"))
            .andExpect(jsonPath("$.exception").value("Exception"))
            .andExpect(jsonPath("$.path").value("/"))
            .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void bodyValidationKeepsDiagnosticsWithoutDebug() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RestExceptionHandler.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            logger.setLevel(Level.INFO);
            var mvc = standaloneSetup(new Probe())
                .setControllerAdvice(new RestExceptionHandler(mock(AppInsights.class), false)).build();
            mvc.perform(post("/validation-probe").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").doesNotExist());
            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getThrowableProxy()).isNotNull();
            });
        } finally {
            logger.setLevel(previous);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @RestController
    static class Probe {
        @GetMapping("/validation-probe")
        public void invalidField() {
            new FieldMapSanitizeOperation().execute(Map.of("invalid field!", "value"));
        }

        @PostMapping("/validation-probe")
        public void invalidBody(@Valid @RequestBody Input input) {
        }
    }

    record Input(@NotBlank String name) {
    }
}
