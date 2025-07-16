package uk.gov.hmcts.ccd.data.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
class DecentralisedUpdateSupplementaryDataResponse {
    private JsonNode supplementaryData;
}
