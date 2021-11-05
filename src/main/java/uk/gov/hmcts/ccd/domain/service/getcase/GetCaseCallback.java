package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.callbacks.GetCaseCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

@Service
public class GetCaseCallback {
    private final CallbackInvoker callbackInvoker;

    public GetCaseCallback(CallbackInvoker callbackInvoker) {
        this.callbackInvoker = callbackInvoker;
    }

    public GetCaseCallbackResponse invoke(final CaseTypeDefinition caseTypeDefinition, final CaseDetails caseDetails) {
        ResponseEntity<GetCaseCallbackResponse> getCaseCallbackResponse = callbackInvoker
            .invokeGetCaseCallback(caseTypeDefinition, caseDetails);

        GetCaseCallbackResponse response = getCaseCallbackResponse.getBody();
        if (response == null) {
            throw new CallbackException(""); // TODO: update the message
        }
        return response;
    }
}
