package uk.gov.hmcts.ccd.feign;

import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.data.SecurityUtils;

import java.util.Date;

@RequiredArgsConstructor
@Slf4j
public class FeignErrorDecoder implements feign.codec.ErrorDecoder {

    private final SecurityUtils securityUtils;

    public static final String CASE_ACTION_PATH = "/audit/caseAction";
    public static final String CASE_SEARCH_PATH = "/audit/caseSearch";

    @Override
    public Exception decode(String methodKey, feign.Response response) {
        int status = response.status();
        FeignException exception = FeignException.errorStatus(methodKey, response);
        log.info("Feign response status: {}, message - {}", status, exception.getMessage());
        if (shouldRetry(response)) {
            if (response.status() == 401 || response.status() == 403) {
                securityUtils.getServiceAuthorization();
            }
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

    private boolean shouldRetry(feign.Response response) {
        int status = response.status();
        // Make sure we retry for POST CaseAction and CaseSearch requests
        return (status == 401 || status == 403 || status == 502 || status == 504)
            && "POST".equalsIgnoreCase(response.request().httpMethod().name())
            && (response.request().url().endsWith(CASE_ACTION_PATH)
            || response.request().url().endsWith(CASE_SEARCH_PATH));
    }

}
