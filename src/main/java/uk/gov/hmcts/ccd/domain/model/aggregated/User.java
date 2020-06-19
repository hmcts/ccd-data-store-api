package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    @JsonProperty("idam")
    private IdamProperties idamProperties;

    public IdamProperties getIdamProperties() {
        return idamProperties;
    }

    public void setIdamProperties(IdamProperties idamProperties) {
        this.idamProperties = idamProperties;
    }
}
