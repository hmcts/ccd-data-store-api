package uk.gov.hmcts.ccd.data.persistence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // For forwards compatibility with future fields
public class DecentralisedCaseEvent {

    private CaseDetails caseDetailsBefore;
    private CaseDetails caseDetails;
    private DecentralisedEventDetails eventDetails;
    private Long internalCaseId;
}
