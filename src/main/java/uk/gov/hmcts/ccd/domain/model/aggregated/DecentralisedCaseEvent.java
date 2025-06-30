package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Data
@Builder
public class POCCaseEvent {

    private CaseDetails caseDetailsBefore;
    private CaseDetails caseDetails;
    private POCEventDetails eventDetails;

    @JsonCreator
    public POCCaseEvent(CaseDetails caseDetailsBefore, CaseDetails caseDetails, POCEventDetails eventDetails) {
        this.caseDetailsBefore = caseDetailsBefore;
        this.caseDetails = caseDetails;
        this.eventDetails = eventDetails;
    }
}
