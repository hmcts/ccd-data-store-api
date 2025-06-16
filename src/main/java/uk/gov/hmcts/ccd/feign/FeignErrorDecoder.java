package uk.gov.hmcts.ccd.feign;

import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Configuration
@Slf4j
public class FeignErrorDecoder implements feign.codec.ErrorDecoder {

    @Override
    public Exception decode(String methodKey, feign.Response response) {
        int status = response.status();
        FeignException exception = FeignException.errorStatus(methodKey, response);
        log.info("Feign response status: {}, message - {}", status, exception.getMessage());
        // Make sure we retry for POST s2s validation only
        if (response.status() >= 400
            && "POST".equalsIgnoreCase(response.request().httpMethod().name())
            && (response.request().url().endsWith("/audit/caseAction") ||
                response.request().url().endsWith("/audit/caseSearch"))) {
            return new RetryableException(
                status,
                exception.getMessage(),
                response.request().httpMethod(),
                (Date) null, // unix timestamp *at which time* the request can be retried
                response.request()
            );
        }
        return exception;
    }

}
