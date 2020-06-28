package uk.gov.hmcts.ccd.domain.service.getcase;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Optional;

public interface GetCaseOperation {

    /**
     * Execute.
     *
     * @param jurisdictionId Case's jurisdiction
     * @param caseTypeId Case's case type
     * @param caseReference 16-digit universally unique case reference
     * @return Optional containing CaseDetails when found; empty optional otherwise
     *
     * @deprecated Use {@link GetCaseOperation#execute(String)} instead
     */
    @Deprecated
    Optional<CaseDetails> execute(final String jurisdictionId, final String caseTypeId, final String caseReference);

    /**
     * Execute.
     *
     * @param caseReference 16-digit universally unique case reference
     * @return Optional containing CaseDetails when found; empty optional otherwise
     */
    Optional<CaseDetails> execute(final String caseReference);
}
