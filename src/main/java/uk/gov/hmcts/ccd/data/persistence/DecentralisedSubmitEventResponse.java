package uk.gov.hmcts.ccd.data.persistence;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class DecentralisedSubmitEventResponse {
    private DecentralisedCaseDetails caseDetails;
    private List<String> errors;
    private List<String> warnings;
    private Boolean ignoreWarning;
}
