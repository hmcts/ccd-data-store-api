package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import lombok.ToString;

@ToString
public class JurisdictionUiConfig implements Serializable {

    private String id = null;
    private Boolean shuttered = null;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getShuttered() {
        return shuttered;
    }

    public void setShuttered(Boolean shuttered) {
        this.shuttered = shuttered;
    }

}
