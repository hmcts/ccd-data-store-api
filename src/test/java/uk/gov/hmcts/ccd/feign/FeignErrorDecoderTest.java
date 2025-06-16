package uk.gov.hmcts.ccd.feign;

import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void shouldReturnRetryableExceptionForPostRequestWithCaseActionUrlAndStatus400() {
        Response response = buildResponse(401, POST_METHOD, CASE_ACTION_URL);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithSearchUrlAndStatus500() {
        Response response = buildResponse(500, POST_METHOD, CASE_SEARCH_URL);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnFeignExceptionForPostRequestWithoutCaseActionOrCaseSerachUrl() {
        Response response = buildResponse(403, POST_METHOD, "http://localhost/service/validate");
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnFeignExceptionForGetRequestEvenWithCaseActionUrl() {
        Response response = buildResponse(401, "GET", CASE_ACTION_URL);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnFeignExceptionForPutRequestWithCaseSearchUrl() {
        Response response = buildResponse(403, "PUT", CASE_SEARCH_URL);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnFeignExceptionForPostRequestWithCaseActionUrlAndStatus200() {
        Response response = buildResponse(200, POST_METHOD, CASE_ACTION_URL);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnFeignExceptionForPostRequestWithCaseActionUrlAndStatus399() {
        Response response = buildResponse(399, POST_METHOD, CASE_ACTION_URL);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseActionInPath() {
        Response response = buildResponse(403, POST_METHOD, "http://localhost/api/v1/audit/caseAction/user");
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseSearchInPath() {
        Response response = buildResponse(403, POST_METHOD, "http://localhost/api/v1/audit/caseSearch/user");
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseActionAsQueryParam() {
        Response response = buildResponse(401, POST_METHOD, "http://localhost/api?endpoint=/audit/caseAction&user=123");
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void shouldReturnRetryableExceptionForPostRequestWithCaseSearchAsQueryParam() {
        Response response = buildResponse(401, POST_METHOD, "http://localhost/api?endpoint=/audit/caseSearch&user=123");
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }
}

