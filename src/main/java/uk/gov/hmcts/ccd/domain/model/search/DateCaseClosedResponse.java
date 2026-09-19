package uk.gov.hmcts.ccd.domain.model.search;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class DateCaseClosedResponse {
    private List<String> caseReferences;

    public DateCaseClosedResponse(List<String> caseReferences) {
        this.caseReferences = caseReferences == null || caseReferences.isEmpty() ? null : caseReferences;
    }
}
