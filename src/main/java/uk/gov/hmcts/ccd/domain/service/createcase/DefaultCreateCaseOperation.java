package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import javax.inject.Inject;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Qualifier("default")
public class DefaultCreateCaseOperation implements CreateCaseOperation {
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

    @Inject
    public DefaultCreateCaseOperation(@Qualifier(DefaultUserRepository.QUALIFIER) final UserRepository userRepository,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                      final EventTriggerService eventTriggerService,
                                      final EventTokenService eventTokenService,
                                      final CaseDataService caseDataService,
                                      final SubmitCaseTransaction submitCaseTransaction,
                                      final CaseSanitiser caseSanitiser,
                                      final CaseTypeService caseTypeService,
                                      final CallbackInvoker callbackInvoker,
                                      final ValidateCaseFieldsOperation validateCaseFieldsOperation) {
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
    }

    @Override
    public CaseDetails createCaseDetails(final String uid,
                                         final String jurisdictionId,
                                         final String caseTypeId,
                                         final Event event,
                                         final Map<String, JsonNode> data,
                                         final Boolean ignoreWarning,
                                         final String token) {

        if (event == null || event.getEventId() == null) {
            throw new ValidationException("Cannot create case because of event is not specified");
        }

        final IDAMProperties idamUser = userRepository.getUserDetails();
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for " + caseTypeId);
        }

        if (!caseTypeService.isJurisdictionValid(jurisdictionId, caseType)) {
            throw new ValidationException("Cannot create case because of " + caseTypeId + " is not defined as case type for " + jurisdictionId);
        }

        final CaseEvent eventTrigger = eventTriggerService.findCaseEvent(caseType, event.getEventId());
        if (eventTrigger == null) {
            throw new ValidationException(event.getEventId() + " is not a known event ID for the specified case type " + caseTypeId);
        }

        if (!eventTriggerService.isPreStateValid(null, eventTrigger)) {
            throw new ValidationException("Cannot create case because of " + eventTrigger.getId() + " has pre-states defined");
        }

        eventTokenService.validateToken(token, uid, eventTrigger, caseType.getJurisdiction(), caseType);

        validateCaseFieldsOperation.validateCaseDetails(jurisdictionId, caseTypeId, event, data);

        final CaseDetails newCaseDetails = new CaseDetails();

        newCaseDetails.setCaseTypeId(caseTypeId);
        newCaseDetails.setJurisdiction(jurisdictionId);
        newCaseDetails.setState(eventTrigger.getPostState());
        newCaseDetails.setSecurityClassification(caseType.getSecurityClassification());
        newCaseDetails.setData(caseSanitiser.sanitise(caseType, data));
        newCaseDetails.setDataClassification(caseDataService.getDefaultSecurityClassifications(caseType, newCaseDetails.getData()));

        final CaseDetails savedCaseDetails = submitCaseTransaction.submitCase(event,
                                                                              caseType,
                                                                              idamUser,
                                                                              eventTrigger,
                                                                              newCaseDetails,
                                                                              ignoreWarning);

        submittedCallback(eventTrigger, savedCaseDetails);

        return savedCaseDetails;
    }

    private void submittedCallback(CaseEvent eventTrigger, CaseDetails savedCaseDetails) {
        if (!isBlank(eventTrigger.getCallBackURLSubmittedEvent())) {
            try { // make a call back
                final ResponseEntity<AfterSubmitCallbackResponse> callBackResponse =
                    callbackInvoker.invokeSubmittedCallback(eventTrigger,
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
