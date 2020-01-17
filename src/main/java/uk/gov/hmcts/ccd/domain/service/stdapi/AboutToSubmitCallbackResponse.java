package uk.gov.hmcts.ccd.domain.service.stdapi;

import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;

import java.util.Optional;

public class AboutToSubmitCallbackResponse {

    private Optional<String> state = Optional.empty();
    private SignificantItem significantItem;

    public Optional<String> getState() {
        return state;
    }

    public void setState(Optional<String> state) {
        this.state = state;
    }

    public SignificantItem getSignificantItem() {
        return significantItem;
    }

    public void setSignificantItem(SignificantItem significantItem) {
        this.significantItem = significantItem;
    }

    @Override
    public String toString() {
        return "AboutToSubmitCallbackResponse{"
            + "state=" + state
            + ", significantItem=" + significantItem
            + '}';
    }
}
