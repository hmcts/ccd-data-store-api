package uk.gov.hmcts.ccd.domain.service.stdapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityValidationService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.domain.service.validate.ValidateSignificantDocument.validateSignificantItem;

@Service
public class CallbackInvoker {

    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();
    private final CallbackService callbackService;
    private final CaseTypeService caseTypeService;
    private final CaseDataService caseDataService;
    private final CaseSanitiser caseSanitiser;
    private final SecurityValidationService securityValidationService;


    @Autowired
    public CallbackInvoker(final CallbackService callbackService,
                           final CaseTypeService caseTypeService,
                           final CaseDataService caseDataService,
                           final CaseSanitiser caseSanitiser,
                           final SecurityValidationService securityValidationService) {
        this.callbackService = callbackService;
        this.caseTypeService = caseTypeService;
        this.caseDataService = caseDataService;
        this.caseSanitiser = caseSanitiser;
        this.securityValidationService = securityValidationService;
    }

    public void invokeAboutToStartCallback(final CaseEvent caseEvent,
                                           final CaseType caseType,
                                           final CaseDetails caseDetails,
                                           final Boolean ignoreWarning) {
        final Optional<CallbackResponse> callbackResponse;
        if (isRetriesDisabled(caseEvent.getRetriesTimeoutAboutToStartEvent())) {
            callbackResponse = callbackService.sendSingleRequest(caseEvent.getCallBackURLAboutToStartEvent(),
                caseEvent, null, caseDetails, false);
        } else {
            callbackResponse = callbackService.send(
                caseEvent.getCallBackURLAboutToStartEvent(),
                caseEvent, null, caseDetails, false);
        }

        callbackResponse.ifPresent(response -> validateAndSetFromAboutToStartCallback(caseType,
            caseDetails,
            ignoreWarning,
            response));
    }

    public AboutToSubmitCallbackResponse invokeAboutToSubmitCallback(final CaseEvent eventTrigger,
                                                                     final CaseDetails caseDetailsBefore,
                                                                     final CaseDetails caseDetails,
                                                                     final CaseType caseType,
                                                                     final Boolean ignoreWarning) {
        final Optional<CallbackResponse> callbackResponse;
        if (isRetriesDisabled(eventTrigger.getRetriesTimeoutURLAboutToSubmitEvent())) {
            callbackResponse = callbackService.sendSingleRequest(eventTrigger.getCallBackURLAboutToSubmitEvent(),
                eventTrigger, caseDetailsBefore, caseDetails, ignoreWarning);
        } else {
            callbackResponse = callbackService.send(
                eventTrigger.getCallBackURLAboutToSubmitEvent(),
                eventTrigger, caseDetailsBefore, caseDetails, ignoreWarning);
        }

        if (callbackResponse.isPresent()) {
            return validateAndSetFromAboutToSubmitCallback(caseType,
                caseDetails,
                ignoreWarning,
                callbackResponse.get());
        }

        return new AboutToSubmitCallbackResponse();
    }

    public ResponseEntity<AfterSubmitCallbackResponse> invokeSubmittedCallback(final CaseEvent eventTrigger,
                                                                               final CaseDetails caseDetailsBefore,
                                                                               final CaseDetails caseDetails) {
        ResponseEntity<AfterSubmitCallbackResponse> afterSubmitCallbackResponseEntity;
        if (isRetriesDisabled(eventTrigger.getRetriesTimeoutURLSubmittedEvent())) {
            afterSubmitCallbackResponseEntity = callbackService.sendSingleRequest(eventTrigger.getCallBackURLSubmittedEvent(),
                eventTrigger,
                caseDetailsBefore,
                caseDetails,
                AfterSubmitCallbackResponse.class);
        } else {
            afterSubmitCallbackResponseEntity = callbackService.send(eventTrigger.getCallBackURLSubmittedEvent(),
                eventTrigger,
                caseDetailsBefore,
                caseDetails,
                AfterSubmitCallbackResponse.class);
        }
        return afterSubmitCallbackResponseEntity;
    }

    public CaseDetails invokeMidEventCallback(final WizardPage wizardPage,
                                              final CaseType caseType,
                                              final CaseEvent caseEvent,
                                              final CaseDetails caseDetailsBefore,
                                              final CaseDetails caseDetails,
                                              final Boolean ignoreWarning) {

        Optional<CallbackResponse> callbackResponseOptional;
        if (isRetriesDisabled(wizardPage.getRetriesTimeoutMidEvent())) {
            callbackResponseOptional = callbackService.sendSingleRequest(wizardPage.getCallBackURLMidEvent(),
                caseEvent,
                caseDetailsBefore,
                caseDetails, false);
        } else {
            callbackResponseOptional = callbackService.send(wizardPage.getCallBackURLMidEvent(),
                caseEvent,
                caseDetailsBefore,
                caseDetails, false);
        }

        if (callbackResponseOptional.isPresent()) {
            CallbackResponse callbackResponse = callbackResponseOptional.get();

            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);
            if (callbackResponse.getData() != null) {
                validateAndSetData(caseType, caseDetails, callbackResponse.getData());
            }
        }

        return caseDetails;
    }

    private boolean isRetriesDisabled(final List<Integer> retriesTimeouts) {
        return retriesTimeouts != null && retriesTimeouts.size() == 1 && retriesTimeouts.get(0) == 0;
    }

    private void validateAndSetFromAboutToStartCallback(CaseType caseType,
                                                        CaseDetails caseDetails,
                                                        Boolean ignoreWarning,
                                                        CallbackResponse callbackResponse) {
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);

        if (callbackResponse.getData() != null) {
            validateAndSetData(caseType, caseDetails, callbackResponse.getData());
        }
    }

    private AboutToSubmitCallbackResponse validateAndSetFromAboutToSubmitCallback(final CaseType caseType,
                                                                                  final CaseDetails caseDetails,
                                                                                  final Boolean ignoreWarning,
                                                                                  final CallbackResponse callbackResponse) {

        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

        validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);
        callbackResponse.updateCallbackStateBasedOnPriority();
        aboutToSubmitCallbackResponse.setState(Optional.ofNullable(callbackResponse.getState()));
        if (callbackResponse.getState() != null) {
            caseDetails.setState(callbackResponse.getState());
        }
        if (callbackResponse.getData() != null) {
            validateAndSetData(caseType, caseDetails, callbackResponse.getData());
            if (callbackResponseHasCaseAndDataClassification(callbackResponse)) {
                securityValidationService.setClassificationFromCallbackIfValid(
                    callbackResponse,
                    caseDetails,
                    deduceDefaultClassificationForExistingFields(caseType, caseDetails)
                );
            }
        }
        return aboutToSubmitCallbackResponse;
    }


    private boolean callbackResponseHasCaseAndDataClassification(CallbackResponse callbackResponse) {
        return (callbackResponse.getSecurityClassification() != null && callbackResponse.getDataClassification() != null) ? true : false;
    }

    private Map<String, JsonNode> deduceDefaultClassificationForExistingFields(CaseType caseType,
                                                                               CaseDetails caseDetails) {
        Map<String, JsonNode> defaultSecurityClassifications = caseDataService.getDefaultSecurityClassifications(
            caseType,
            caseDetails.getData(),
            EMPTY_DATA_CLASSIFICATION);
        return defaultSecurityClassifications;
    }

    private void validateAndSetData(final CaseType caseType,
                                    final CaseDetails caseDetails,
                                    final Map<String, JsonNode> responseData) {
        caseTypeService.validateData(responseData, caseType);
        caseDetails.setData(caseSanitiser.sanitise(caseType, responseData));
        deduceDataClassificationForNewFields(caseType, caseDetails);
    }

    private void deduceDataClassificationForNewFields(CaseType caseType, CaseDetails caseDetails) {
        Map<String, JsonNode> defaultSecurityClassifications = caseDataService.getDefaultSecurityClassifications(
            caseType,
            caseDetails.getData(),
            ofNullable(caseDetails.getDataClassification()).orElse(
                newHashMap()));
        caseDetails.setDataClassification(defaultSecurityClassifications);
    }
}
