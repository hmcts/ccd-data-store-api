package uk.gov.hmcts.ccd.domain.service.common;

import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.List;
import java.util.Optional;

public interface SecurityClassificationService {

    Optional<CaseDetails> applyClassification(CaseDetails caseDetails);

    List<AuditEvent> applyClassification(CaseDetails caseDetails, List<AuditEvent> events);

    SecurityClassification getClassificationForEvent(CaseTypeDefinition caseTypeDefinition,
                                                     CaseEventDefinition caseEventDefinition);

    boolean userHasEnoughSecurityClassificationForField(String jurisdictionId,
                                                        CaseTypeDefinition caseTypeDefinition, String fieldId);

    boolean userHasEnoughSecurityClassificationForField(String jurisdictionId,
                                                        SecurityClassification otherClassification);

    Optional<SecurityClassification> getUserClassification(String jurisdictionId);

    Optional<SecurityClassification>  getUserClassification(CaseDetails caseDetails, boolean create);

}
