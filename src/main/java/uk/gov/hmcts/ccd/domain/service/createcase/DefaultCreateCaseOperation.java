package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CasePostStateService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Qualifier("default")
public class DefaultCreateCaseOperation implements CreateCaseOperation {
    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();
    private final UserRepository userRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final EventTriggerService eventTriggerService;
    private final EventTokenService eventTokenService;
    private final CaseDataService caseDataService;
    private final SubmitCaseTransaction submitCaseTransaction;
    private final CaseSanitiser caseSanitiser;
    private final CaseTypeService caseTypeService;
    private final CallbackInvoker callbackInvoker;
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final DraftGateway draftGateway;
    private final CasePostStateService casePostStateService;

    @Inject
    public DefaultCreateCaseOperation(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                      final CaseDefinitionRepository caseDefinitionRepository,
                                      final EventTriggerService eventTriggerService,
                                      final EventTokenService eventTokenService,
                                      final CaseDataService caseDataService,
                                      final SubmitCaseTransaction submitCaseTransaction,
                                      final CaseSanitiser caseSanitiser,
                                      final CaseTypeService caseTypeService,
                                      final CallbackInvoker callbackInvoker,
                                      final ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                      final CasePostStateService casePostStateService,
                                      @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway) {
        this.userRepository = userRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.eventTriggerService = eventTriggerService;
        this.eventTokenService = eventTokenService;
        this.submitCaseTransaction = submitCaseTransaction;
        this.caseSanitiser = caseSanitiser;
        this.caseTypeService = caseTypeService;
        this.caseDataService = caseDataService;
        this.callbackInvoker = callbackInvoker;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.casePostStateService = casePostStateService;
        this.draftGateway = draftGateway;
    }

    @Override
    public CaseDetails createCaseDetails(final String caseTypeId,
                                         final CaseDataContent caseDataContent,
                                         final Boolean ignoreWarning) {
        Event event = caseDataContent.getEvent();
        if (event == null || event.getEventId() == null) {
            throw new ValidationException("Cannot create case because of event is not specified");
        }

        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for " + caseTypeId);
        }

        final CaseEventDefinition caseEventDefinition =
            eventTriggerService.findCaseEvent(caseTypeDefinition, event.getEventId());
        if (caseEventDefinition == null) {
            throw new ValidationException(event.getEventId() + " is not a known event ID for the specified case type "
                + caseTypeId);
        }

        if (!eventTriggerService.isPreStateValid(null, caseEventDefinition)) {
            throw new ValidationException("Cannot create case because of " + caseEventDefinition.getId()
                + " has pre-states defined");
        }

        String token = caseDataContent.getToken();
        eventTokenService.validateToken(token, userRepository.getUserId(),
            caseEventDefinition,
            caseTypeDefinition.getJurisdictionDefinition(),
            caseTypeDefinition);

        validateCaseFieldsOperation.validateCaseDetails(caseTypeId, caseDataContent);

        final CaseDetails newCaseDetails = new CaseDetails();

        newCaseDetails.setCaseTypeId(caseTypeId);
        newCaseDetails.setJurisdiction(caseTypeDefinition.getJurisdictionId());
        newCaseDetails.setSecurityClassification(caseTypeDefinition.getSecurityClassification());
        Map<String, JsonNode> data = caseDataContent.getData();
        newCaseDetails.setData(caseSanitiser.sanitise(caseTypeDefinition, data));
        newCaseDetails.setDataClassification(caseDataService.getDefaultSecurityClassifications(
            caseTypeDefinition,
            newCaseDetails.getData(),
            EMPTY_DATA_CLASSIFICATION));
        updateCaseState(caseEventDefinition, newCaseDetails);

        final IdamUser idamUser = userRepository.getUser();
        final CaseDetails savedCaseDetails = submitCaseTransaction.submitCase(event,
                                                                              caseTypeDefinition,
                                                                              idamUser,
                                                                              caseEventDefinition,
                                                                              newCaseDetails,
                                                                              ignoreWarning);

        submittedCallback(caseEventDefinition, savedCaseDetails);

        deleteDraft(caseDataContent, savedCaseDetails);

        return savedCaseDetails;
    }

    private void updateCaseState(CaseEventDefinition caseEventDefinition, CaseDetails newCaseDetails) {
        newCaseDetails.setState(this.casePostStateService
            .evaluateCaseState(caseEventDefinition, newCaseDetails));
    }

    private void deleteDraft(CaseDataContent caseDataContent, CaseDetails savedCaseDetails) {
        if (StringUtils.isNotBlank(caseDataContent.getDraftId())) {
            try {
                draftGateway.delete(Draft.stripId(caseDataContent.getDraftId()));
                savedCaseDetails.setDeleteDraftResponseEntity(caseDataContent.getDraftId(),
                    ResponseEntity.ok().build());
            } catch (Exception e) {
                savedCaseDetails.setIncompleteDeleteDraftResponse();
            }
        }
    }

    private void submittedCallback(CaseEventDefinition caseEventDefinition, CaseDetails savedCaseDetails) {
        if (!isBlank(caseEventDefinition.getCallBackURLSubmittedEvent())) {
            try { // make a call back
                final ResponseEntity<AfterSubmitCallbackResponse> callBackResponse =
                    callbackInvoker.invokeSubmittedCallback(caseEventDefinition,
                                                            null,
                                                            savedCaseDetails);
                savedCaseDetails.setAfterSubmitCallbackResponseEntity(callBackResponse);
            } catch (CallbackException ex) {
                // Exception occurred, e.g. call back service is unavailable
                savedCaseDetails.setIncompleteCallbackResponse();
            }
        }
    }
}
