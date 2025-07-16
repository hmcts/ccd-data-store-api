package uk.gov.hmcts.ccd.feign;

import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class FeignErrorDecoder implements feign.codec.ErrorDecoder {
    public static final String CASE_ACTION_PATH = "/audit/caseAction";
    public static final String CASE_SEARCH_PATH = "/audit/caseSearch";

    @Override
    public Exception decode(String methodKey, feign.Response response) {
        int status = response.status();
        FeignException exception = FeignException.errorStatus(methodKey, response);
        log.info("Feign response status: {}, message - {}", status, exception.getMessage());
        // Make sure we retry for POST CaseAction and CaseSearch requests
        if (response.status() == 401
            && "POST".equalsIgnoreCase(response.request().httpMethod().name())
            && (response.request().url().endsWith(CASE_ACTION_PATH)
                || response.request().url().endsWith(CASE_SEARCH_PATH)
            )) {
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
