package uk.gov.hmcts.ccd.decentralised.dto;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Data;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class DecentralisedUpdateSupplementaryDataResponse {
    private JsonNode supplementaryData;
}
