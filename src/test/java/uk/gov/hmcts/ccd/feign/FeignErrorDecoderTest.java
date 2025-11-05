package uk.gov.hmcts.ccd.feign;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.Request;
import feign.Request.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.SecurityUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;

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

    @ParameterizedTest(name = "[{index}] status={0}, method={1}, url={2} → Retryable")
    @MethodSource("retryableCases")
    @DisplayName("should return RetryableException for qualifying POSTs to caseAction/caseSearch")
    void shouldReturnRetryableException(int status, String method, String url) {
        Response response = buildResponse(status, method, url);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(RetryableException.class);
    }

    @ParameterizedTest(name = "[{index}] status={0}, method={1}, url={2} → NOT Retryable")
    @MethodSource("nonRetryableCases")
    @DisplayName("should return FeignException (non-retryable) for all other cases")
    void shouldReturnFeignException(int status, String method, String url) {
        Response response = buildResponse(status, method, url);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex)
            .isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    private static Stream<Arguments> retryableCases() {
        return Stream.of(
            // caseAction (POST)
            Arguments.of(401, POST_METHOD, CASE_ACTION_URL),
            Arguments.of(403, POST_METHOD, CASE_ACTION_URL),
            Arguments.of(502, POST_METHOD, CASE_ACTION_URL),
            Arguments.of(504, POST_METHOD, CASE_ACTION_URL),

            // caseSearch (POST)
            Arguments.of(401, POST_METHOD, CASE_SEARCH_URL),
            Arguments.of(403, POST_METHOD, CASE_SEARCH_URL),
            Arguments.of(502, POST_METHOD, CASE_SEARCH_URL),
            Arguments.of(504, POST_METHOD, CASE_SEARCH_URL)
        );
    }

    private static Stream<Arguments> nonRetryableCases() {
        return Stream.of(
            // 500s on both endpoints (POST)
            Arguments.of(500, POST_METHOD, CASE_ACTION_URL),
            Arguments.of(500, POST_METHOD, CASE_SEARCH_URL),

            // wrong URLs or methods
            Arguments.of(403, POST_METHOD, "http://localhost/service/validate"),
            Arguments.of(401, "GET", CASE_ACTION_URL),
            Arguments.of(403, "PUT", CASE_SEARCH_URL),

            // non-retryable status codes
            Arguments.of(200, POST_METHOD, CASE_ACTION_URL),
            Arguments.of(399, POST_METHOD, CASE_ACTION_URL),

            // endpoints only in path or query params
            Arguments.of(403, POST_METHOD, "http://localhost/api/v1/audit/caseAction/user"),
            Arguments.of(403, POST_METHOD, "http://localhost/api/v1/audit/caseSearch/user"),
            Arguments.of(401, POST_METHOD, "http://localhost/api?endpoint=/audit/caseAction&user=123"),
            Arguments.of(401, POST_METHOD, "http://localhost/api?endpoint=/audit/caseSearch&user=123")
        );
    }
}

