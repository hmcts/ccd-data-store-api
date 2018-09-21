package uk.gov.hmcts.ccd.domain.service.stdapi;

import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;

import java.util.Optional;

public class AboutToSubmitCallbackResponse {

    private Optional<String> callBackResponse = Optional.empty();
    private SignificantItem significantItem;

    public Optional<String> getCallBackResponse() {
        return callBackResponse;
    }

    public void setCallBackResponse(Optional<String> callBackResponse) {
        this.callBackResponse = callBackResponse;
    }

    public SignificantItem getSignificantItem() {
        return significantItem;
    }

    public void setSignificantItem(SignificantItem significantItem) {
        this.significantItem = significantItem;
    }

    @Override
    public String toString() {
        return "AboutToSubmitCallbackResponse{" +
            "callBackResponse=" + callBackResponse +
            ", significantItem=" + significantItem +
            '}';
    }
}
