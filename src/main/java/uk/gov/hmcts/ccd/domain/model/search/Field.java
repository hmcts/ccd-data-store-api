package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

public class Field {
    private String id;
    @JsonProperty("field_type")
    private FieldType type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }
}
