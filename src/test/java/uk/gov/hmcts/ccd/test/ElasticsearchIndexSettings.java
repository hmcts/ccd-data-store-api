package uk.gov.hmcts.ccd.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class ElasticsearchIndexSettings {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Map<String, JsonNode> types;
    private final Optional<JsonNode> settings;
    private final Optional<JsonNode> aliases;

    public ElasticsearchIndexSettings(Optional<InputStream> settings, Optional<InputStream> aliases) {
        this.types = new HashMap<>();
        this.settings = settings.map(s -> rawToJson(unwrapIO(s)));
        this.aliases = aliases.map(a -> rawToJson(unwrapIO(a)));
    }

    public void addType(String type, InputStream mapping) {
        types.put(type, rawToJson(unwrapIO(mapping)).get(type));
    }

    public ObjectNode toJson() {
        ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
        objectNode.set("settings", settings.orElse(OBJECT_MAPPER.createObjectNode()));
        objectNode.set("aliases", aliases.orElse(OBJECT_MAPPER.createObjectNode()));
        ObjectNode mappingsObject = OBJECT_MAPPER.createObjectNode();
        mappingsObject.setAll(types);
        objectNode.set("mappings", mappingsObject);
        return objectNode;
    }

    public static String unwrapIO(InputStream stream) {
        try {
            return IOUtils.toString(stream, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error during reading response body", e);
        }
    }

    public static JsonNode rawToJson(String rawJson) {
        try {
            return OBJECT_MAPPER.readTree(rawJson);
        } catch (IOException e) {
            throw new RuntimeException("Problem converting to JSON", e);
        }
    }
}
