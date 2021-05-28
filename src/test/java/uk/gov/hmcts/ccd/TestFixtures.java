package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.provider.Arguments;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class TestFixtures {
    protected static final String JURISDICTION = "SSCS";
    protected static final String CASE_REFERENCE = "1234123412341236";
    protected static final Long REFERENCE = Long.valueOf(CASE_REFERENCE);

    protected static final Map<String, String> MAP_A = Map.of("a", "A");
    protected static final Map<String, String> MAP_B = Map.of("b", "B");

    protected static final DocumentHashToken HASH_TOKEN_A = DocumentHashToken.builder().id("a").hashToken("A").build();
    protected static final DocumentHashToken HASH_TOKEN_B = DocumentHashToken.builder().id("b").hashToken("B").build();

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    protected Map<String, JsonNode> loadDataAsMap(final String fileName) throws IOException {
        final InputStream inputStream = getInputStream(fileName);
        final TypeReference<Map<String, JsonNode>> typeReference = new TypeReference<>() {
        };
        return OBJECT_MAPPER.readValue(inputStream, typeReference);
    }

    @SuppressWarnings("SameParameterValue")
    protected List<JsonNode> loadDataAsList(final String fileName) throws IOException {
        final InputStream inputStream = getInputStream(fileName);
        final TypeReference<List<JsonNode>> typeReference = new TypeReference<>() {
        };
        return OBJECT_MAPPER.readValue(inputStream, typeReference);
    }

    @SuppressWarnings("unused")
    protected static Stream<Arguments> provideNullMapParameters() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(MAP_A, null),
            Arguments.of(null, MAP_B)
        );
    }

    private InputStream getInputStream(final String fileName) {
        return TestFixtures.class.getClassLoader()
            .getResourceAsStream("tests/".concat(fileName));
    }

}
