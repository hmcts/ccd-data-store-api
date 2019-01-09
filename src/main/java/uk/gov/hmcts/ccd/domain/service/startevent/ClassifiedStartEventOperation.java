package uk.gov.hmcts.ccd.domain.service.startevent;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
@Qualifier("classified")
public class ClassifiedStartEventOperation implements StartEventOperation {
    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();
    private final StartEventOperation startEventOperation;
    private final SecurityClassificationService classificationService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataService caseDataService;
    private final DraftGateway draftGateway;

    public ClassifiedStartEventOperation(@Qualifier("default") StartEventOperation startEventOperation,
                                         SecurityClassificationService classificationService,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         final CaseDataService caseDataService,
                                         @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway) {
        this.startEventOperation = startEventOperation;
        this.classificationService = classificationService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDataService = caseDataService;
        this.draftGateway = draftGateway;
    }

    @Override
    public StartEventTrigger triggerStartForCaseType(String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        return startEventOperation.triggerStartForCaseType(caseTypeId,
                                                           eventTriggerId,
                                                           ignoreWarning);
    }

    @Override
    public StartEventTrigger triggerStartForCase(String caseReference, String eventTriggerId, Boolean ignoreWarning) {
        return applyClassificationIfCaseDetailsExist(startEventOperation.triggerStartForCase(caseReference,
                                                                                             eventTriggerId,
                                                                                             ignoreWarning));
    }

    @Override
    public StartEventTrigger triggerStartForDraft(String draftReference,
                                                  Boolean ignoreWarning) {
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        return applyClassificationIfCaseDetailsExist(deduceDefaultClassificationsForDraft(startEventOperation.triggerStartForDraft(draftReference,
                                                                                                                                   ignoreWarning),
                                                                                          caseDetails.getCaseTypeId()));
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
