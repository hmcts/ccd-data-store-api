package uk.gov.hmcts.ccd.domain.service.startevent;

import java.util.HashMap;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.JcLogger;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
@Qualifier("classified")
public class ClassifiedStartEventOperation implements StartEventOperation {

    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();
    private final StartEventOperation startEventOperation;
    private final SecurityClassificationServiceImpl classificationService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataService caseDataService;
    private final DraftGateway draftGateway;

    final JcLogger jcLogger = new JcLogger("ClassifiedStartEventOperation");

    public ClassifiedStartEventOperation(@Qualifier("default") StartEventOperation startEventOperation,
                                         SecurityClassificationServiceImpl classificationService,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                         final CaseDefinitionRepository caseDefinitionRepository,
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
        jcLogger.jclog("triggerStartForCaseType()");
        return startEventOperation.triggerStartForCaseType(caseTypeId,
                                                           eventId,
                                                           ignoreWarning);
    }

    @Override
    public StartEventResult triggerStartForCase(String caseReference, String eventId, Boolean ignoreWarning) {
        jcLogger.jclog("triggerStartForCase() [ENTRYPOINT , built 13-Jan-2025]");
        jcLogger.jclog("triggerStartForCase() [caseReference = " + caseReference + " , eventId = " + eventId + "]");
        jcLogger.jclog("triggerStartForCase() CALL STACK = " + JcLogger.getStackTraceAsString(new Exception()));
        StartEventResult startEventResult = startEventOperation.triggerStartForCase(caseReference, eventId,
                                                                                    ignoreWarning);
        jcLogger.jclog("triggerStartForCase() startEventResult", startEventResult);
        StartEventResult startEventResult2 = applyClassificationIfCaseDetailsExist(caseReference, startEventResult);
        jcLogger.jclog("triggerStartForCase() startEventResult2", startEventResult2);
        return startEventResult2;
    }

    @Override
    public StartEventResult triggerStartForDraft(String draftReference,
                                                 Boolean ignoreWarning) {
        jcLogger.jclog("triggerStartForDraft()");
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        return applyClassificationIfCaseDetailsExist(draftReference,
            deduceDefaultClassificationsForDraft(startEventOperation
                .triggerStartForDraft(draftReference, ignoreWarning), caseDetails.getCaseTypeId()));
    }

    private StartEventResult deduceDefaultClassificationsForDraft(StartEventResult startEventResult,
                                                                  String caseTypeId) {
        jcLogger.jclog("deduceDefaultClassificationsForDraft()");
        CaseDetails caseDetails = startEventResult.getCaseDetails();
        deduceDefaultClassificationIfCaseDetailsPresent(caseTypeId, caseDetails);
        return startEventResult;
    }

    private void deduceDefaultClassificationIfCaseDetailsPresent(String caseTypeId, CaseDetails caseDetails) {
        jcLogger.jclog("deduceDefaultClassificationIfCaseDetailsPresent()");
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

    private StartEventResult applyClassificationIfCaseDetailsExist(String caseReference,
                                                                   StartEventResult startEventResult) {
        jcLogger.jclog("applyClassificationIfCaseDetailsExist() [#2]");
        CaseDetails caseDetails = startEventResult.getCaseDetails();
        if (null != caseDetails) {
            if (caseDetails.getSecurityClassification() == SecurityClassification.RESTRICTED) {
                jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE RESTRICTED CASE 1]",
                    caseDetails);
                jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE RESTRICTED CASE 2]",
                    caseDetails.hashCode());
                Optional<CaseDetails> caseDetails1 = classificationService
                    .applyClassificationToRestrictedCase(caseDetails);
                if (caseDetails1.isPresent()) {
                    jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE RESTRICTED CASE 3]",
                        caseDetails1.get());
                    jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE RESTRICTED CASE 4]",
                        caseDetails1.get().hashCode());
                    jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE RESTRICTED CASE 5]",
                        caseDetails1.get().getSecurityClassification());
                }
                startEventResult.setCaseDetails(caseDetails1
                    .orElseThrow(() -> new CaseNotFoundException(caseReference)));
            } else {
                jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE NORMAL CASE 1]",
                    caseDetails);
                jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE NORMAL CASE 2]",
                    caseDetails.hashCode());
                Optional<CaseDetails> caseDetails1 = classificationService.applyClassification(caseDetails);
                if (caseDetails1.isPresent()) {
                    jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE NORMAL CASE 3]",
                        caseDetails1.get());
                    jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE NORMAL CASE 4]",
                        caseDetails1.get().hashCode());
                    jcLogger.jclog("applyClassificationIfCaseDetailsExist() [HANDLE NORMAL CASE 5]",
                        caseDetails1.get().getSecurityClassification());
                }
                startEventResult.setCaseDetails(caseDetails1
                    .orElseThrow(() -> new CaseNotFoundException(caseReference)));
            }
        }
        return startEventResult;
    }
}
