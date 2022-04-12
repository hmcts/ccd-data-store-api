package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;

public abstract class AbstractCaseController {
    private final GetCaseOperation getCaseOperation;
    private final UIDService caseReferenceService;

    public AbstractCaseController(@Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
                                  final UIDService caseReferenceService) {
        this.getCaseOperation = getCaseOperation;
        this.caseReferenceService = caseReferenceService;
    }

    protected void validateCaseReference(final String caseReference) {
        if (!caseReferenceService.validateUID(caseReference)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }
    }

    protected CaseDetails getCaseDetails(final String caseReference) {
        return getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }
}
