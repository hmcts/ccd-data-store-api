package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

@Setter
@Getter
public class CriteriaInput implements CommonDCPModel {
    private String label;
    private int order;
    private Field field;
    private String role;
    // Classification for V2 input responses; excluded from legacy criteria JSON.
    @JsonIgnore
    private String dataType;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;

    public String buildCaseFieldId() {
        if (this.field != null) {
            return StringUtils.isEmpty(this.field.getElementPath()) ? this.field.getId() :
                this.field.getId() + '.' + this.field.getElementPath();
        }
        return null;
    }

}
