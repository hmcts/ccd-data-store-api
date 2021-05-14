package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SupplementaryData {

    private static final Logger LOG = LoggerFactory.getLogger(SupplementaryData.class);

    @JsonIgnore
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> response;

    public SupplementaryData(String caseReference, JsonNode data, Set<String> requestKeys) {
        if (requestKeys == null || requestKeys.isEmpty()) {
            LOG.error(String.format("Case reference: %s, No requests keys", caseReference));
            this.response = JacksonUtils.convertJsonNode(data);
        } else {
            ParseContext parseContext =
                JsonPath.using(Configuration.defaultConfiguration().setOptions(Option.SUPPRESS_EXCEPTIONS));
            DocumentContext context = parseContext.parse(jsonNodeToString(data));

            this.response = new HashMap<>();
            LOG.error(String.format("Case reference: %s, Processing below keys,", caseReference));
            requestKeys.forEach(key -> {
                LOG.error(String.format("Case reference: %s, Key: %s", caseReference, key));
                Object value = context.read("$." + key, Object.class);
                if (value != null) {
                    this.response.put(key, value);
                }
            });
        }
    }

    private String jsonNodeToString(JsonNode data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Unable to map object to JSON string", e);
        }
    }
}
