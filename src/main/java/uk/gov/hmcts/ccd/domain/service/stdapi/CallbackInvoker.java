package uk.gov.hmcts.ccd.domain.service.stdapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.GetCaseCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityValidationService;
import uk.gov.hmcts.ccd.domain.service.processor.GlobalSearchProcessorService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.GET_CASE;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.MID_EVENT;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.SUBMITTED;
import static uk.gov.hmcts.ccd.domain.service.validate.ValidateSignificantDocument.validateSignificantItem;

@Service
public class CallbackInvoker {

    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();

    private final CallbackService callbackService;
    private final CaseTypeService caseTypeService;
    private final CaseDataService caseDataService;
    private final CaseSanitiser caseSanitiser;
    private final SecurityValidationService securityValidationService;
    private final GlobalSearchProcessorService globalSearchProcessorService;
    private final TimeToLiveService timeToLiveService;

    @Autowired
    public CallbackInvoker(final CallbackService callbackService,
                           final CaseTypeService caseTypeService,
                           final CaseDataService caseDataService,
                           final CaseSanitiser caseSanitiser,
                           final SecurityValidationService securityValidationService,
                           final GlobalSearchProcessorService globalSearchProcessorService,
                           final TimeToLiveService timeToLiveService) {
        this.callbackService = callbackService;
        this.caseTypeService = caseTypeService;
        this.caseDataService = caseDataService;
        this.caseSanitiser = caseSanitiser;
        this.securityValidationService = securityValidationService;
        this.globalSearchProcessorService = globalSearchProcessorService;
        this.timeToLiveService = timeToLiveService;
    }

    public void invokeAboutToStartCallback(final CaseEventDefinition caseEventDefinition,
                                           final CaseTypeDefinition caseTypeDefinition,
                                           final CaseDetails caseDetails,
                                           final Boolean ignoreWarning) {
        final Optional<CallbackResponse> callbackResponse;
        if (isRetriesDisabled(caseEventDefinition.getRetriesTimeoutAboutToStartEvent())) {
            callbackResponse = callbackService.sendSingleRequest(caseEventDefinition.getCallBackURLAboutToStartEvent(),
                ABOUT_TO_START, caseEventDefinition, null, caseDetails, false);
        } else {
            callbackResponse = callbackService.send(
                caseEventDefinition.getCallBackURLAboutToStartEvent(), ABOUT_TO_START,
                caseEventDefinition, null, caseDetails, false);
        }

        callbackResponse.ifPresent(response -> validateAndSetFromAboutToStartCallback(caseTypeDefinition,
            caseDetails,
            ignoreWarning,
            response));
    }

    public AboutToSubmitCallbackResponse invokeAboutToSubmitCallback(final CaseEventDefinition caseEventDefinition,
                                                                     final CaseDetails caseDetailsBefore,
                                                                     final CaseDetails caseDetails,
                                                                     final CaseTypeDefinition caseTypeDefinition,
                                                                     final Boolean ignoreWarning) {
        final Optional<CallbackResponse> callbackResponse;
        if (isRetriesDisabled(caseEventDefinition.getRetriesTimeoutURLAboutToSubmitEvent())) {
            callbackResponse = callbackService.sendSingleRequest(caseEventDefinition.getCallBackURLAboutToSubmitEvent(),
                ABOUT_TO_SUBMIT, caseEventDefinition, caseDetailsBefore, caseDetails, ignoreWarning);
        } else {
            callbackResponse = callbackService.send(
                caseEventDefinition.getCallBackURLAboutToSubmitEvent(), ABOUT_TO_SUBMIT,
                caseEventDefinition, caseDetailsBefore, caseDetails, ignoreWarning);
        }

        return callbackResponse.map(response -> validateAndSetFromAboutToSubmitCallback(caseTypeDefinition,
                caseDetails,
                ignoreWarning,
                response)).orElseGet(AboutToSubmitCallbackResponse::new);

    }

    public ResponseEntity<AfterSubmitCallbackResponse> invokeSubmittedCallback(final CaseEventDefinition
                                                                                   caseEventDefinition,
                                                                               final CaseDetails caseDetailsBefore,
                                                                               final CaseDetails caseDetails) {
        ResponseEntity<AfterSubmitCallbackResponse> afterSubmitCallbackResponseEntity;
        if (isRetriesDisabled(caseEventDefinition.getRetriesTimeoutURLSubmittedEvent())) {
            afterSubmitCallbackResponseEntity =
                callbackService.sendSingleRequest(caseEventDefinition.getCallBackURLSubmittedEvent(),
                    SUBMITTED, caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    AfterSubmitCallbackResponse.class);
        } else {
            afterSubmitCallbackResponseEntity = callbackService.send(caseEventDefinition.getCallBackURLSubmittedEvent(),
                SUBMITTED, caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                AfterSubmitCallbackResponse.class);
        }
        return afterSubmitCallbackResponseEntity;
    }

    public ResponseEntity<GetCaseCallbackResponse> invokeGetCaseCallback(final CaseTypeDefinition caseTypeDefinition,
                                                                         final CaseDetails caseDetails) {
        String url = caseTypeDefinition.getCallbackGetCaseUrl();
        List<Integer> retries = caseTypeDefinition.getRetriesGetCaseUrl();

        CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("GetCaseCallback");
        caseEventDefinition.setName("GetCaseCallback");

        ResponseEntity<GetCaseCallbackResponse> getCaseCallbackResponseEntity;
        if (isRetriesDisabled(retries)) {
            getCaseCallbackResponseEntity =
                callbackService.sendSingleRequest(url,
                    GET_CASE, caseEventDefinition,
                    null,
                    caseDetails,
                    GetCaseCallbackResponse.class);
        } else {
            getCaseCallbackResponseEntity = callbackService.send(url,
                GET_CASE, caseEventDefinition,
                null,
                caseDetails,
                GetCaseCallbackResponse.class);
        }
        return getCaseCallbackResponseEntity;
    }

    public CaseDetails invokeMidEventCallback(final WizardPage wizardPage,
                                              final CaseTypeDefinition caseTypeDefinition,
                                              final CaseEventDefinition caseEventDefinition,
                                              final CaseDetails caseDetailsBefore,
                                              final CaseDetails caseDetails,
                                              final Boolean ignoreWarning) {

        Optional<CallbackResponse> callbackResponseOptional;
        if (isRetriesDisabled(wizardPage.getRetriesTimeoutMidEvent())) {
            callbackResponseOptional = callbackService.sendSingleRequest(wizardPage.getCallBackURLMidEvent(),
                MID_EVENT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails, false);
        } else {
            callbackResponseOptional = callbackService.send(wizardPage.getCallBackURLMidEvent(),
                MID_EVENT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails, false);
        }

        if (callbackResponseOptional.isPresent()) {
            CallbackResponse callbackResponse = callbackResponseOptional.get();

            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);
            if (callbackResponse.getData() != null) {
                validateAndSetData(caseTypeDefinition, caseDetails, callbackResponse.getData());
            }
        }

        return caseDetails;
    }

    private boolean isRetriesDisabled(final List<Integer> retriesTimeouts) {
        return retriesTimeouts != null && retriesTimeouts.size() == 1 && retriesTimeouts.get(0) == 0;
    }

    private void validateAndSetFromAboutToStartCallback(CaseTypeDefinition caseTypeDefinition,
                                                        CaseDetails caseDetails,
                                                        Boolean ignoreWarning,
                                                        CallbackResponse callbackResponse) {
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);

        if (callbackResponse.getData() != null) {
            validateAndSetData(caseTypeDefinition, caseDetails, callbackResponse.getData());
        }
    }

    private AboutToSubmitCallbackResponse validateAndSetFromAboutToSubmitCallback(final CaseTypeDefinition
                                                                                      caseTypeDefinition,
                                                                                  final CaseDetails caseDetails,
                                                                                  final Boolean ignoreWarning,
                                                                                  final CallbackResponse
                                                                                      callbackResponse) {

        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

        validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);
        callbackResponse.updateCallbackStateBasedOnPriority();
        aboutToSubmitCallbackResponse.setState(Optional.ofNullable(callbackResponse.getState()));
        if (callbackResponse.getState() != null) {
            caseDetails.setState(callbackResponse.getState());
        }
        if (callbackResponse.getData() != null) {
            validateAndSetDataForGlobalSearch(caseTypeDefinition, caseDetails, callbackResponse.getData());
            if (callbackResponseHasCaseAndDataClassification(callbackResponse)) {
                securityValidationService.setClassificationFromCallbackIfValid(
                    callbackResponse,
                    caseDetails,
                    deduceDefaultClassificationForExistingFields(caseTypeDefinition, caseDetails)
                );
            }
        }
        return aboutToSubmitCallbackResponse;
    }


    private boolean callbackResponseHasCaseAndDataClassification(CallbackResponse callbackResponse) {
        return (callbackResponse.getSecurityClassification() != null
            && callbackResponse.getDataClassification() != null);
    }

    private Map<String, JsonNode> deduceDefaultClassificationForExistingFields(CaseTypeDefinition caseTypeDefinition,
                                                                               CaseDetails caseDetails) {
        return caseDataService.getDefaultSecurityClassifications(caseTypeDefinition,
                                                                caseDetails.getData(),
                                                                EMPTY_DATA_CLASSIFICATION);
    }

    private void validateAndSetData(final CaseTypeDefinition caseTypeDefinition,
                                    final CaseDetails caseDetails,
                                    final Map<String, JsonNode> responseData,
                                    final boolean populateGlobalSearch) {
        timeToLiveService.verifyTTLContentNotChangedByCallback(caseDetails.getData(), responseData);
        caseTypeService.validateData(responseData, caseTypeDefinition);

        Map<String, JsonNode> responseDataToSanitise = responseData;

        if (populateGlobalSearch) {
            responseDataToSanitise =
                globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, responseData);
        }

        caseDetails.setData(caseSanitiser.sanitise(caseTypeDefinition, responseDataToSanitise));
        deduceDataClassificationForNewFields(caseTypeDefinition, caseDetails);
    }

    private void validateAndSetData(final CaseTypeDefinition caseTypeDefinition,
                                    final CaseDetails caseDetails,
                                    final Map<String, JsonNode> responseData) {
        validateAndSetData(caseTypeDefinition, caseDetails, responseData, false);
    }

    private void validateAndSetDataForGlobalSearch(final CaseTypeDefinition caseTypeDefinition,
                                    final CaseDetails caseDetails,
                                    final Map<String, JsonNode> responseData) {

        validateAndSetData(caseTypeDefinition, caseDetails, responseData, true);
    }

    private void deduceDataClassificationForNewFields(CaseTypeDefinition caseTypeDefinition, CaseDetails caseDetails) {
        Map<String, JsonNode> defaultSecurityClassifications = caseDataService.getDefaultSecurityClassifications(
            caseTypeDefinition,
            caseDetails.getData(),
            ofNullable(caseDetails.getDataClassification()).orElse(
                newHashMap()));
        caseDetails.setDataClassification(defaultSecurityClassifications);
    }
}
