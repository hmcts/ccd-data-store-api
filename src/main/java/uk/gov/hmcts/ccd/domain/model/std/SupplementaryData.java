package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.ccd.config.JacksonUtils;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SupplementaryData {

    private Map<String, Object> response;

    public SupplementaryData(JsonNode data) {
        this.response = JacksonUtils.convertJsonNode(data);
    }
}
