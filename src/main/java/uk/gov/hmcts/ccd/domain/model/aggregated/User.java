package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    @JsonProperty("idam")
    private IDAMProperties idamProperties;

    public IDAMProperties getIdamProperties() {
        return idamProperties;
    }

    public void setIdamProperties(IDAMProperties idamProperties) {
        this.idamProperties = idamProperties;
    }
}
