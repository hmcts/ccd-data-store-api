package uk.gov.hmcts.ccd.decentralised.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class DecentralisedSubmitEventResponse {
    @JsonUnwrapped
    private DecentralisedCaseDetails caseDetails;
    private List<String> errors;
    private List<String> warnings;
    private Boolean ignoreWarning;
}
