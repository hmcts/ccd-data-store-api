package uk.gov.hmcts.ccd.domain.service.stdapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityValidationService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;

import java.util.Map;
import java.util.Optional;

@Service
public class CallbackInvoker {

    private static final String CALLBACK_RESPONSE_KEY_STATE = "state";
    private final CallbackService callbackService;
    private final CaseTypeService caseTypeService;
    private final CaseDataService caseDataService;
    private final CaseSanitiser caseSanitiser;
    private final SecurityClassificationService securityClassificationService;
    private final SecurityValidationService securityValidationService;

    @Autowired
    public CallbackInvoker(final CallbackService callbackService,
                           final CaseTypeService caseTypeService,
                           final CaseDataService caseDataService,
                           final CaseSanitiser caseSanitiser,
                           final SecurityClassificationService securityClassificationService,
                           final SecurityValidationService securityValidationService) {
        this.callbackService = callbackService;
        this.caseTypeService = caseTypeService;
        this.caseDataService = caseDataService;
        this.caseSanitiser = caseSanitiser;
        this.securityClassificationService = securityClassificationService;
        this.securityValidationService = securityValidationService;
    }

    public void invokeAboutToStartCallback(final CaseEvent caseEvent,
                                           final CaseType caseType,
                                           final CaseDetails caseDetails,
                                           final Boolean ignoreWarning) {
        final Optional<CallbackResponse> callbackResponse = callbackService.send(
            caseEvent.getCallBackURLAboutToStartEvent(),
            caseEvent.getRetriesTimeoutAboutToStartEvent(),
            caseEvent, caseDetails);

        callbackResponse.ifPresent(response -> validateAndSetData(caseType,
                                                                  caseDetails,
                                                                  ignoreWarning,
                                                                  response));
    }

    public Optional<String> invokeAboutToSubmitCallback(final CaseEvent eventTrigger,
                                            final CaseDetails caseDetailsBefore,
                                            final CaseDetails caseDetails,
                                            final CaseType caseType,
                                            final Boolean ignoreWarning) {
        final Optional<CallbackResponse> callbackResponse = callbackService.send(
            eventTrigger.getCallBackURLAboutToSubmitEvent(),
            eventTrigger.getRetriesTimeoutURLAboutToSubmitEvent(),
            eventTrigger, caseDetailsBefore, caseDetails);
        return callbackResponse.flatMap(response -> validateSubmitCallback(caseType,
                                                                           caseDetails,
                                                                           ignoreWarning,
                                                                           response));
    }

    public ResponseEntity<AfterSubmitCallbackResponse> invokeSubmittedCallback(final CaseEvent eventTrigger,
                                                                               final CaseDetails caseDetailsBefore,
                                                                               final CaseDetails caseDetails) {
        return callbackService.send(eventTrigger.getCallBackURLSubmittedEvent(),
                                    eventTrigger.getRetriesTimeoutURLSubmittedEvent(),
                                    eventTrigger,
                                    caseDetailsBefore,
                                    caseDetails,
                                    AfterSubmitCallbackResponse.class);
    }

    void validateAndSetData(CaseType caseType, CaseDetails caseDetails, Boolean ignoreWarning, CallbackResponse callbackResponse) {
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);

        if (callbackResponse.getData() != null) {
            validateAndSetData(caseType, caseDetails, callbackResponse.getData());
        }
    }

    private void validateAndSetData(final CaseType caseType,
                                    final CaseDetails caseDetails,
                                    final Map<String, JsonNode> responseData) {
        caseTypeService.validateData(responseData, caseType);
        caseDetails.setData(caseSanitiser.sanitise(caseType, responseData));
        caseDetails.setDataClassification(caseDataService.getDefaultSecurityClassifications(caseType, caseDetails.getData()));
    }

    Optional<String> validateSubmitCallback(final CaseType caseType,
                                            final CaseDetails caseDetails,
                                            final Boolean ignoreWarning,
                                            final CallbackResponse callbackResponse) {
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);
        if (callbackResponse.getData() != null) {
            validateAndSetData(caseType, caseDetails, callbackResponse.getData());
            securityValidationService.validateCallbackClassification(callbackResponse, caseDetails);
            final Optional<String> newCaseState = filterCaseState(callbackResponse.getData());
            newCaseState.ifPresent(caseDetails::setState);
            return newCaseState;
        }
        return Optional.empty();
    }

    Optional<String> filterCaseState(final Map<String, JsonNode> data) {
        final Optional<JsonNode> jsonNode = Optional.ofNullable(data.get(CALLBACK_RESPONSE_KEY_STATE));
        jsonNode.ifPresent(value -> data.remove(CALLBACK_RESPONSE_KEY_STATE));
        return jsonNode.flatMap(value -> value.isTextual() ? Optional.of(value.textValue()) : Optional.empty());
    }
}
