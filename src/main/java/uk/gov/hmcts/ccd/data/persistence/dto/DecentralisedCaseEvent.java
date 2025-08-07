package uk.gov.hmcts.ccd.data.persistence.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@Builder
public class DecentralisedCaseEvent {

    private CaseDetails caseDetailsBefore;
    private CaseDetails caseDetails;
    private DecentralisedEventDetails eventDetails;

    @JsonCreator
    public DecentralisedCaseEvent(CaseDetails caseDetailsBefore, CaseDetails caseDetails,
                                  DecentralisedEventDetails eventDetails) {
        this.caseDetailsBefore = caseDetailsBefore;
        this.caseDetails = caseDetails;
        this.eventDetails = eventDetails;
    }
}
