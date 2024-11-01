package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Data
@Builder
public class POCCaseDetails {

    private CaseDetails caseDetails;
    private POCEventDetails eventDetails;

    @JsonCreator
    public POCCaseDetails(CaseDetails caseDetails, POCEventDetails eventDetails) {
        this.caseDetails = caseDetails;
        this.eventDetails = eventDetails;
    }
}
