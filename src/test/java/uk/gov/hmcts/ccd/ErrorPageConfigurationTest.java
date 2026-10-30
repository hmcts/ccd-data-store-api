package uk.gov.hmcts.ccd;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorPageConfigurationTest {

    @Test
    void fallbackOmitsDiagnosticsEvenWhenRequested() throws Exception {

        ErrorProperties properties = new Binder(new MapConfigurationPropertySource(
            PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))))
            .bind("server.error", Bindable.of(ErrorProperties.class)).get();
        final BasicErrorController controller = new BasicErrorController(
            new ErrorPageConfiguration().safeErrorAttributes(), properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/private/case-123");
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException("internal diagnostic"));
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "internal diagnostic");
        request.setParameter("trace", "true");
        request.setParameter("message", "true");
        request.setParameter("errors", "true");
        assertThat(controller.error(request).getBody())
            .doesNotContainKeys("exception", "trace", "message", "errors", "path");
        assertThat(controller.error(request).getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void browserPageDoesNotRenderTheErrorModel() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        new ErrorPageConfiguration().errorView().render(Map.of("message", "internal diagnostic",
            "trace", "java.lang.SecretException", "path", "http://internal-service/private"),
            new MockHttpServletRequest(), response);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("Request failed")
            .doesNotContain("internal diagnostic", "SecretException", "internal-service", "Whitelabel");
    }
}
