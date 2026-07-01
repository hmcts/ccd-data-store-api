package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class FixedListItemDefinition implements Serializable, Copyable<FixedListItemDefinition> {

    private static final long serialVersionUID = 6196146295016140921L;
    private String code = null;
    private String label = null;
    private String order = null;

    @Schema
    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Schema
    @JsonProperty("order")
    public String getOrder() {
        return order;
    }

    public void setOrder(final String order) {
        this.order = order;
    }

    @JsonIgnore
    @Override
    public FixedListItemDefinition createCopy() {
        FixedListItemDefinition copy = new FixedListItemDefinition();
        copy.setCode(this.code);
        copy.setLabel(this.label);
        copy.setOrder(this.order);
        return copy;
    }
}
