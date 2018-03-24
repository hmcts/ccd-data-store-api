package uk.gov.hmcts.ccd.domain.model.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AfterSubmitCallbackResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonProperty("confirmation_header")
    private String confirmationHeader;

    @JsonProperty("confirmation_body")
    private String confirmationBody;

    public String getConfirmationHeader() {
        return confirmationHeader;
    }

    public void setConfirmationHeader(final String confirmationHeader) {
        this.confirmationHeader = confirmationHeader;
    }

    public String getConfirmationBody() {
        return confirmationBody;
    }

    public void setConfirmationBody(final String confirmationBody) {
        this.confirmationBody = confirmationBody;
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return toString();
        }
    }
}
