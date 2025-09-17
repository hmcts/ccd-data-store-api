package uk.gov.hmcts.ccd.feign;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.Request;
import feign.Request.HttpMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.SecurityUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"PMD.CloseResource"})

@ExtendWith(MockitoExtension.class)
class FeignErrorDecoderTest {

    private static final String POST_METHOD = "POST";
    private static final String CASE_ACTION_URL = "http://localhost/service/audit/caseAction";
    private static final String CASE_SEARCH_URL = "http://localhost/service/audit/caseSearch";
    private static final String METHOD_KEY = "methodKey";

    @Mock
    private SecurityUtils securityUtils;


    @InjectMocks
    private FeignErrorDecoder feignErrorDecoder;


    private Response buildResponse(int status, String method, String url) {
        Request request = Request.create(
            HttpMethod.valueOf(method),
            url,
            Collections.emptyMap(),
            null,
            StandardCharsets.UTF_8,
            null
        );
        return Response.builder()
            .status(status)
            .request(request)
            .build();
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseActionUrlAndStatus401() {
        isRetryableException(401, POST_METHOD, CASE_ACTION_URL);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseActionUrlAndStatus403() {
        isRetryableException(403, POST_METHOD, CASE_ACTION_URL);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseActionUrlAndStatus502() {
        isRetryableException(502, POST_METHOD, CASE_ACTION_URL);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseActionUrlAndStatus504() {
        isRetryableException(504, POST_METHOD, CASE_ACTION_URL);
    }

    @Test
    void shouldReturnFeignExceptionForPostRequestWithActionUrlAndStatus500() {
        isNotRetryableException(500, POST_METHOD, CASE_ACTION_URL);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseSearchUrlAndStatus401() {
        isRetryableException(401, POST_METHOD, CASE_SEARCH_URL);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseSearchUrlAndStatus403() {
        isRetryableException(403, POST_METHOD, CASE_SEARCH_URL);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseSearchUrlAndStatus502() {
        isRetryableException(502, POST_METHOD, CASE_SEARCH_URL);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseSearchUrlAndStatus504() {
        isRetryableException(504, POST_METHOD, CASE_SEARCH_URL);
    }

    @Test
    void shouldReturnFeignExceptionForPostRequestWithSearchUrlAndStatus500() {
        isNotRetryableException(500, POST_METHOD, CASE_SEARCH_URL);
    }

    @Test
    void shouldReturnFeignExceptionForPostRequestWithoutCaseActionOrCaseSerachUrl() {
        isNotRetryableException(403, POST_METHOD, "http://localhost/service/validate");
    }

    @Test
    void shouldReturnFeignExceptionForGetRequestEvenWithCaseActionUrl() {
        isNotRetryableException(401, "GET", CASE_ACTION_URL);
    }

    @Test
    void shouldReturnFeignExceptionForPutRequestWithCaseSearchUrl() {
        isNotRetryableException(403, "PUT", CASE_SEARCH_URL);
    }

    @Test
    void shouldReturnFeignExceptionForPostRequestWithCaseActionUrlAndStatus200() {
        isNotRetryableException(200, POST_METHOD, CASE_ACTION_URL);
    }

    @Test
    void shouldReturnFeignExceptionForPostRequestWithCaseActionUrlAndStatus399() {
        isNotRetryableException(399, POST_METHOD, CASE_ACTION_URL);
    }

    @Test
    void shouldNotReturnRetryableExceptionForPostRequestWithCaseActionInPath() {
        isNotRetryableException(403, POST_METHOD, "http://localhost/api/v1/audit/caseAction/user");
    }

    @Test
    void shouldNotReturnRetryableExceptionForPostRequestWithCaseSearchInPath() {
        isNotRetryableException(403, POST_METHOD, "http://localhost/api/v1/audit/caseSearch/user");
    }

    @Test
    void shouldNotReturnRetryableExceptionForPostRequestWithCaseActionAsQueryParam() {
        isNotRetryableException(401, POST_METHOD, "http://localhost/api?endpoint=/audit/caseAction&user=123");
    }

    @Test
    void shouldNotReturnRetryableExceptionForPostRequestWithCaseSearchAsQueryParam() {
        isNotRetryableException(401, POST_METHOD, "http://localhost/api?endpoint=/audit/caseSearch&user=123");
    }

    private void isNotRetryableException(int status, String method, String url) {
        Response response = buildResponse(status, method, url);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    private void isRetryableException(int status, String method, String url) {
        Response response = buildResponse(status, method, url);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(RetryableException.class);
    }
}

