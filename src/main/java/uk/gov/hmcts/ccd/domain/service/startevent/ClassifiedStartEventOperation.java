package uk.gov.hmcts.ccd.domain.service.startevent;

import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
@Qualifier("classified")
public class ClassifiedStartEventOperation implements StartEventOperation {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifiedStartEventOperation.class);

    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();
    private final StartEventOperation startEventOperation;
    private final SecurityClassificationServiceImpl classificationService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataService caseDataService;
    private final DraftGateway draftGateway;

    final ObjectMapper objectMapper = new ObjectMapper();

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

        // Enables serialisation of java.util.Optional and java.time.LocalDateTime
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    private void jclog(String message) {
        LOG.info("JCDEBUG: ClassifiedStartEventOperation: {}", message);
    }

    private void jclog(String message, StartEventResult startEventResult) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(startEventResult));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR: " + e.getMessage());
        }
    }

    @Override
    public StartEventResult triggerStartForCaseType(String caseTypeId, String eventId, Boolean ignoreWarning) {
        return startEventOperation.triggerStartForCaseType(caseTypeId,
                                                           eventId,
                                                           ignoreWarning);
    }

    @Override
    public StartEventResult triggerStartForCase(String caseReference, String eventId, Boolean ignoreWarning) {
        jclog("triggerStartForCase (ENTRY-POINT , caseReference = " + caseReference + " , eventId = " + eventId + ")");
        StartEventResult startEventResult = startEventOperation.triggerStartForCase(caseReference, eventId,
                                                                                    ignoreWarning);
        jclog("    startEventResult", startEventResult);
        StartEventResult startEventResult2 = applyClassificationIfCaseDetailsExist(caseReference, startEventResult);
        jclog("    startEventResult2", startEventResult2);
        return startEventResult2;
    }

    @Override
    public StartEventResult triggerStartForDraft(String draftReference,
                                                 Boolean ignoreWarning) {
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        return applyClassificationIfCaseDetailsExist(draftReference,
            deduceDefaultClassificationsForDraft(startEventOperation
                .triggerStartForDraft(draftReference, ignoreWarning), caseDetails.getCaseTypeId()));
    }

    private StartEventResult deduceDefaultClassificationsForDraft(StartEventResult startEventResult,
                                                                  String caseTypeId) {
        jclog("deduceDefaultClassificationsForDraft");
        CaseDetails caseDetails = startEventResult.getCaseDetails();
        deduceDefaultClassificationIfCaseDetailsPresent(caseTypeId, caseDetails);
        return startEventResult;
    }

    private void deduceDefaultClassificationIfCaseDetailsPresent(String caseTypeId, CaseDetails caseDetails) {
        jclog("deduceDefaultClassificationIfCaseDetailsPresent");
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

    // PART OF FIX.
    private StartEventResult applyClassificationIfCaseDetailsExist(String caseReference,
                                                                   StartEventResult startEventResult) {
        jclog("applyClassificationIfCaseDetailsExist (#2)");
        CaseDetails caseDetails = startEventResult.getCaseDetails();
        if (null != caseDetails) {
            if (caseDetails.getSecurityClassification() == SecurityClassification.RESTRICTED) {
                jclog("applyClassificationIfCaseDetailsExist (handle RESTRICTED case)");
                startEventResult.setCaseDetails(classificationService.applyClassificationToRestrictedCase(caseDetails)
                    .orElseThrow(() -> new CaseNotFoundException(caseReference)));
            } else {
                jclog("applyClassificationIfCaseDetailsExist (handle NORMAL case)");
                startEventResult.setCaseDetails(classificationService.applyClassification(caseDetails)
                    .orElseThrow(() -> new CaseNotFoundException(caseReference)));
            }
        }
        return startEventResult;
    }
}
