package uk.gov.hmcts.ccd.domain.service.startevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;

@Service
@Qualifier("classified")
public class ClassifiedStartEventOperation implements StartEventOperation {
    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();
    private final StartEventOperation startEventOperation;
    private final SecurityClassificationService classificationService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataService caseDataService;


    public ClassifiedStartEventOperation(@Qualifier("default") StartEventOperation startEventOperation,
                                         SecurityClassificationService classificationService,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         final CaseDataService caseDataService) {
        this.startEventOperation = startEventOperation;
        this.classificationService = classificationService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDataService = caseDataService;
    }

    @Override
    public StartEventTrigger triggerStartForCaseType(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        return startEventOperation.triggerStartForCaseType(uid,
                                                           jurisdictionId,
                                                           caseTypeId,
                                                           eventTriggerId,
                                                           ignoreWarning);
    }

    @Override
    public StartEventTrigger triggerStartForCase(String uid, String jurisdictionId, String caseTypeId, String caseReference, String eventTriggerId, Boolean ignoreWarning) {
        return applyClassificationIfCaseDetailsExist(startEventOperation.triggerStartForCase(uid,
                                                                                             jurisdictionId,
                                                                                             caseTypeId,
                                                                                             caseReference,
                                                                                             eventTriggerId,
                                                                                             ignoreWarning));
    }

    @Override
    public StartEventTrigger triggerStartForDraft(String uid, String jurisdictionId, String caseTypeId, String draftReference, String eventTriggerId,
                                                  Boolean ignoreWarning) {
        return applyClassificationIfCaseDetailsExist(deduceDefaultClassificationsForDraft(startEventOperation.triggerStartForDraft(uid,
                                                                                                                                   jurisdictionId,
                                                                                                                                   caseTypeId,
                                                                                                                                   draftReference,
                                                                                                                                   eventTriggerId,
                                                                                                                                   ignoreWarning),
                                                                                          caseTypeId));
    }

    private StartEventTrigger deduceDefaultClassificationsForDraft(StartEventTrigger startEventTrigger, String caseTypeId) {
        CaseDetails caseDetails = startEventTrigger.getCaseDetails();
        deduceDefaultClassificationIfCaseDetailsPresent(caseTypeId, caseDetails);
        return startEventTrigger;
    }

    private void deduceDefaultClassificationIfCaseDetailsPresent(String caseTypeId, CaseDetails caseDetails) {
        if (null != caseDetails) {
            final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
            if (caseType == null) {
                throw new ValidationException("Cannot find case type definition for " + caseTypeId);
            }
            caseDetails.setSecurityClassification(caseType.getSecurityClassification());
            caseDetails.setDataClassification(caseDataService.getDefaultSecurityClassifications(caseType, caseDetails.getData(), EMPTY_DATA_CLASSIFICATION));
        }
    }

    private StartEventTrigger applyClassificationIfCaseDetailsExist(StartEventTrigger startEventTrigger) {
        CaseDetails caseDetails = startEventTrigger.getCaseDetails();
        if (null != caseDetails) {
            startEventTrigger.setCaseDetails(classificationService.applyClassification(caseDetails).orElse(null));
        }
        return startEventTrigger;
    }
}
