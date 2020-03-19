package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.HashMap;

public class JacksonUtils {

     public static final  JsonFactory jsonFactory = JsonFactory.builder()
        // Change per-factory setting to prevent use of `String.intern()` on symbols
        .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
        // Disable json-specific setting to produce non-standard "unquoted" field names:
        .disable(JsonWriteFeature.QUOTE_FIELD_NAMES)
        .build();

    public static final  ObjectMapper MAPPER_INSTANCE = JsonMapper.builder(jsonFactory)
        .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
        .build();

    public static final TypeReference  getStringToMap() {
        return new TypeReference<HashMap<String, JsonNode>>() {};
    }
}
