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
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
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
    public StartEventResult triggerStartForCaseType(String caseTypeId, String eventId, Boolean ignoreWarning) {
        return startEventOperation.triggerStartForCaseType(caseTypeId,
                                                           eventId,
                                                           ignoreWarning);
    }

    @Override
    public StartEventResult triggerStartForCase(String caseReference, String eventId, Boolean ignoreWarning) {
        return applyClassificationIfCaseDetailsExist(startEventOperation.triggerStartForCase(caseReference,
                                                                                             eventId,
                                                                                             ignoreWarning));
    }

    @Override
    public StartEventResult triggerStartForDraft(String draftReference,
                                                 Boolean ignoreWarning) {
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        return applyClassificationIfCaseDetailsExist(deduceDefaultClassificationsForDraft(startEventOperation.triggerStartForDraft(draftReference,
                                                                                                                                   ignoreWarning),
                                                                                          caseDetails.getCaseTypeId()));
    }

    private StartEventResult deduceDefaultClassificationsForDraft(StartEventResult startEventResult, String caseTypeId) {
        CaseDetails caseDetails = startEventResult.getCaseDetails();
        deduceDefaultClassificationIfCaseDetailsPresent(caseTypeId, caseDetails);
        return startEventResult;
    }

    private void deduceDefaultClassificationIfCaseDetailsPresent(String caseTypeId, CaseDetails caseDetails) {
        if (null != caseDetails) {
            final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
            if (caseTypeDefinition == null) {
                throw new ValidationException("Cannot find case type definition for " + caseTypeId);
            }
            caseDetails.setSecurityClassification(caseTypeDefinition.getSecurityClassification());
            caseDetails.setDataClassification(caseDataService.getDefaultSecurityClassifications(caseTypeDefinition,
                caseDetails.getData(),
                EMPTY_DATA_CLASSIFICATION));
        }
    }

    private StartEventResult applyClassificationIfCaseDetailsExist(StartEventResult startEventResult) {
        CaseDetails caseDetails = startEventResult.getCaseDetails();
        if (null != caseDetails) {
            startEventResult.setCaseDetails(classificationService.applyClassification(caseDetails).orElse(null));
        }
        return startEventResult;
    }
}
