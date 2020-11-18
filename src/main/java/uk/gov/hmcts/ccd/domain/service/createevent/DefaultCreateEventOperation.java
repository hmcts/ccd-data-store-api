package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import javax.inject.Inject;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Qualifier("default")
public class DefaultCreateEventOperation implements CreateEventOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCreateEventOperation.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private final EventValidator eventValidator;
    private final CreateCaseEventService createEventService;
    private final CallbackInvoker callbackInvoker;

    @Inject
    public DefaultCreateEventOperation(final EventValidator eventValidator,
                                       final CreateCaseEventService createEventService,
                                       final CallbackInvoker callbackInvoker) {
        this.createEventService = createEventService;
        this.eventValidator = eventValidator;
        this.callbackInvoker = callbackInvoker;
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public CaseDetails createCaseEvent(final String caseReference,
                                       final CaseDataContent content) {
        eventValidator.validate(content.getEvent());

        CreateCaseEventResult caseEventResult = createEventService.createCaseEvent(caseReference, content);

        if (!isBlank(caseEventResult.getEventTrigger().getCallBackURLSubmittedEvent())) {
            return invokeSubmitedToCallback(caseEventResult);
        }
        return caseEventResult.getSavedCaseDetails();
    }

    private CaseDetails invokeSubmitedToCallback(CreateCaseEventResult caseEventResult) {
        CaseDetails caseDetails = caseEventResult.getSavedCaseDetails();
        try { // make a call back
            final ResponseEntity<AfterSubmitCallbackResponse> callBackResponse = callbackInvoker
                .invokeSubmittedCallback(caseEventResult.getEventTrigger(), caseEventResult.getCaseDetailsBefore(),
                    caseEventResult.getSavedCaseDetails());
            caseDetails.setAfterSubmitCallbackResponseEntity(callBackResponse);
        } catch (CallbackException ex) {
            LOG.warn("Submitted callback failed", ex);
            // Exception occurred, e.g. call back service is unavailable,
            caseDetails.setIncompleteCallbackResponse();
        }
        return caseDetails;
    }

}
