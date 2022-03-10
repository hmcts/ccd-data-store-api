package uk.gov.hmcts.ccd.domain.service.common;

import java.util.List;
import java.util.Optional;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

public interface SecurityClassificationService {

    Optional<CaseDetails> applyClassification(CaseDetails caseDetails);

    List<AuditEvent> applyClassification(CaseDetails caseDetails, List<AuditEvent> events);

    SecurityClassification getClassificationForEvent(CaseTypeDefinition caseTypeDefinition,
                                                     CaseEventDefinition caseEventDefinition);

    boolean userHasEnoughSecurityClassificationForField(String jurisdictionId,
                                                        CaseTypeDefinition caseTypeDefinition,
                                                        String fieldId);

    boolean userHasEnoughSecurityClassificationForField(CaseTypeDefinition caseTypeDefinition,
                                                        SecurityClassification otherClassification);

    Optional<SecurityClassification> getUserClassification(CaseTypeDefinition caseTypeDefinition,
                                                           boolean isCreateProfile);

    Optional<SecurityClassification>  getUserClassification(CaseDetails caseDetails, boolean create);

}
