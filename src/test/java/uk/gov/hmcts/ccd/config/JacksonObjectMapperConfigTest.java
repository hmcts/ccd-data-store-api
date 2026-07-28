package uk.gov.hmcts.ccd.config;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

@DisplayName("JacksonObjectMapperConfig")
class JacksonObjectMapperConfigTest {

    private static final String DATE_TIME_ISO8601 = "2017-03-01T10:20:30";
    private static final LocalDateTime DATE_TIME = LocalDateTime.parse(DATE_TIME_ISO8601);
    private static final String DATE_TIME_ARRAY = "[2017,3,1,10,20,30]";
    private static final String USER_INFO_JSON = """
        {
          "sub": "caseworker@example.com",
          "uid": "123",
          "name": "Case Worker",
          "given_name": "Case",
          "family_name": "Worker",
          "roles": ["caseworker", "caseworker-test"]
        }
        """;
    private static final String AM_DOCUMENT_JSON = """
        {
          "classification": "RESTRICTED",
          "size": 42,
          "mimeType": "application/pdf",
          "originalDocumentName": "evidence.pdf"
        }
        """;

    private JacksonObjectMapperConfig mapperConfig;

    @BeforeEach
    void setUp() {
        mapperConfig = new JacksonObjectMapperConfig();
    }

    @Test
    @DisplayName("should configure a default ObjectMapper")
    void shouldConfigureDefaultObjectMapper() {
        final ObjectMapper mapper = mapperConfig.defaultObjectMapper();

        assertAll(
            () -> assertThat(mapper.writeValueAsString(DATE_TIME), equalTo("\"" + DATE_TIME_ISO8601 + "\"")),
            () -> assertThat(mapper.isEnabled(StreamReadFeature.STRICT_DUPLICATE_DETECTION), is(true)),
            () -> assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS), is(false))
        );
    }

    @Test
    @DisplayName("should use constructor parameter names when deserialising")
    void shouldUseConstructorParameterNamesWhenDeserialising() {
        final ObjectMapper mapper = mapperConfig.defaultObjectMapper();

        UserInfo userInfo = mapper.readValue(USER_INFO_JSON, UserInfo.class);

        assertAll(
            () -> assertThat(userInfo.getSub(), equalTo("caseworker@example.com")),
            () -> assertThat(userInfo.getUid(), equalTo("123")),
            () -> assertThat(userInfo.getName(), equalTo("Case Worker")),
            () -> assertThat(userInfo.getGivenName(), equalTo("Case")),
            () -> assertThat(userInfo.getFamilyName(), equalTo("Worker")),
            () -> assertThat(userInfo.getRoles(), equalTo(List.of("caseworker", "caseworker-test")))
        );
    }

    @Test
    @DisplayName("should deserialize the published AM document model without a Jackson 2 parameter-names module")
    void shouldDeserializePublishedAmDocumentModelWithJackson3() {
        final ObjectMapper mapper = mapperConfig.defaultObjectMapper();

        Document document = mapper.readValue(AM_DOCUMENT_JSON, Document.class);

        assertAll(
            () -> assertThat(document.classification, equalTo(Classification.RESTRICTED)),
            () -> assertThat(document.size, equalTo(42L)),
            () -> assertThat(document.mimeType, equalTo("application/pdf")),
            () -> assertThat(document.originalDocumentName, equalTo("evidence.pdf"))
        );
    }

    @Test
    @DisplayName("should configure a simple ObjectMapper")
    void shouldConfigureSimpleObjectMapper() {
        final ObjectMapper mapper = mapperConfig.simpleObjectMapper();

        assertThat(mapper.writeValueAsString(DATE_TIME), equalTo(DATE_TIME_ARRAY));
    }

}
