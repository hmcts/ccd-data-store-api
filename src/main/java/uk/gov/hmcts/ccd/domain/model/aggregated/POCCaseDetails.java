package uk.gov.hmcts.ccd.domain.model.aggregated;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Data
@Builder
public class POCCaseDetails {

    private CaseDetails caseDetails;
    private POCEventDetails eventDetails;
}
