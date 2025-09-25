package uk.gov.hmcts.ccd.domain.service.validate;

import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public record OperationContext(String caseTypeId, CaseDataContent content, String pageId) {
    public OperationContext(String caseTypeId, CaseDataContent content) {
        this(caseTypeId, content, null);
    }
}
