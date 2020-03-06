package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;

import java.util.Optional;

public interface GetCaseDocumentOperation {
    /**
     *
     * @param caseReference 16-digit universally unique case reference
     * @return Optional containing CaseDetails when found; empty optional otherwise
     */
    CaseDocumentMetadata execute(final String caseReference, final String documentId);
}
