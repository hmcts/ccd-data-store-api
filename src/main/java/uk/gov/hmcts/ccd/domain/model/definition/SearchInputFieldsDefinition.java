package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@ToString
@ApiModel(description = "")
public class SearchInputFieldsDefinition implements Serializable, CommonDCPModel {
    private static final Logger LOG = LoggerFactory.getLogger(SearchInputFieldsDefinition.class);

    private String caseTypeId = null;
    private List<SearchInputField> fields = new ArrayList<>();
    private String displayContextParameter = null;

    private void jcdebug(String message) {
        LOG.debug("JCDEBUG: SearchInputFieldsDefinition: debug: {}", message);
        LOG.info("JCDEBUG: SearchInputFieldsDefinition: info:  {}", message);
        LOG.warn("JCDEBUG: SearchInputFieldsDefinition: warn:  {}", message);
    }

    @ApiModelProperty(value = "")
    @JsonProperty("case_type_id")
    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("fields")
    public List<SearchInputField> getFields() {
        return fields;
    }

    public void setFields(List<SearchInputField> fields) {
        try {
            throw new RuntimeException("JCDEBUG: SearchInputFieldsDefinition.setFields");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            jcdebug("setFields: " + sw.toString());
        }
        this.fields = fields;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("display_context_parameter")
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

}
