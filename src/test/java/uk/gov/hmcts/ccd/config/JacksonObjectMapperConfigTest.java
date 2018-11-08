package uk.gov.hmcts.ccd.config;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JacksonObjectMapperConfig")
class JacksonObjectMapperConfigTest {

    private static final String DATE_TIME_ISO8601 = "2017-03-01T10:20:30";
    private static final LocalDateTime DATE_TIME = LocalDateTime.parse(DATE_TIME_ISO8601);
    private static final String DATE_TIME_ARRAY = "[2017,3,1,10,20,30]";

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
            () -> assertThat(mapper.canSerialize(LocalDateTime.class), is(true)),
            () -> assertThat(mapper.writeValueAsString(DATE_TIME), equalTo("\"" + DATE_TIME_ISO8601 + "\""))
        );
    }

    @Test
    @DisplayName("should configure a simple ObjectMapper")
    void shouldConfigureSimpleObjectMapper() {
        final ObjectMapper mapper = mapperConfig.simpleObjectMapper();

        assertAll(
            () -> assertThat(mapper.canSerialize(LocalDateTime.class), is(true)),
            () -> assertThat(mapper.writeValueAsString(DATE_TIME), equalTo(DATE_TIME_ARRAY))
        );
    }

}
