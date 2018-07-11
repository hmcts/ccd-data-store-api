package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class CaseDataContentBuilder {
    private Event event;
    private Map<String, JsonNode> data;
    private Map<String, JsonNode> securityClassification;
    private String token;
    private Boolean ignoreWarning;

    public CaseDataContentBuilder withEvent(Event event) {
        this.event = event;
        return this;
    }

    public CaseDataContentBuilder withData(Map<String, JsonNode> data) {
        this.data = data;
        return this;
    }

    public CaseDataContentBuilder withSecurityClassification(Map<String, JsonNode> securityClassification) {
        this.securityClassification = securityClassification;
        return this;
    }

    public CaseDataContentBuilder withToken(String token) {
        this.token = token;
        return this;
    }

    public CaseDataContentBuilder withIgnoreWarning(Boolean ignoreWarning) {
        this.ignoreWarning = ignoreWarning;
        return this;
    }

    public static CaseDataContentBuilder aCaseDataContent() {
        return new CaseDataContentBuilder();
    }

    public CaseDataContent build() {
        return new CaseDataContent(event, data, securityClassification, token, ignoreWarning);
    }
}
