package uk.gov.hmcts.ccd.decentralised.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // For forwards compatibility with future fields
public class DecentralisedCaseDetails {
    private CaseDetails caseDetails;
    private Long revision;
}
