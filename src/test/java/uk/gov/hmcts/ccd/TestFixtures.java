package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class TestFixtures {
     public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
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
    protected static Stream<Arguments> provideMapsParameters() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(Map.of("a", "1"), null),
            Arguments.of(null, Map.of("b", "2"))
        );
    }

    private InputStream getInputStream(final String fileName) {
        return TestFixtures.class.getClassLoader()
            .getResourceAsStream("tests/".concat(fileName));
    }

}
