package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

public class Field {
    private String id;
    private String elementPath;
    @JsonProperty("field_type")
    private FieldTypeDefinition type;
    private boolean metadata;
    @JsonProperty("show_condition")
    private String showCondition;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getElementPath() {
        return elementPath;
    }

    public void setElementPath(String elementPath) {
        this.elementPath = elementPath;
    }

    public FieldTypeDefinition getType() {
        return type;
    }

    public void setType(FieldTypeDefinition type) {
        this.type = type;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }
}
