package uk.gov.hmcts.ccd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.BeanNameViewResolver;

import java.util.Map;

@Configuration
public class ErrorPageConfiguration {

    @Bean
    public DefaultErrorAttributes safeErrorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
                Map<String, Object> attributes = super.getErrorAttributes(request, options);
                attributes.remove("path");
                attributes.remove("exception");
                attributes.remove("message");
                attributes.remove("trace");
                attributes.remove("errors");

                return attributes;
            }
        };
    }

    @Bean
    public BeanNameViewResolver safeErrorViewResolver() {
        BeanNameViewResolver resolver = new BeanNameViewResolver();
        resolver.setOrder(0);
        return resolver;
    }

    @Bean(name = "error")
    public View errorView() {
        return new AbstractView() {
            @Override
            protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                                   HttpServletResponse response) throws Exception {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("<!doctype html><html lang=\"en\"><head><title>Request failed</title>"
                    + "</head><body><h1>Request failed</h1><p>Unable to process this request.</p></body></html>");
            }
        };
    }
}
