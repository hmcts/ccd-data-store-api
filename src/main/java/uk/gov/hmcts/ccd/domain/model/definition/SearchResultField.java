package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SearchResultField implements Serializable {
    @JsonProperty("case_type_id")
    private String caseTypeId;
    @JsonProperty("case_field_id")
    private String caseFieldId;
    @JsonProperty("case_field_element_path")
    private String caseFieldPath = null;
    private String label;
    @JsonProperty("order")
    private Integer displayOrder;
    private boolean metadata;
    @JsonProperty("role")
    private String role;
    @JsonProperty("sort_order")
    private SortOrder sortOrder;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter = null;

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getCaseFieldId() {
        return caseFieldId;
    }

    public void setCaseFieldId(String caseFieldId) {
        this.caseFieldId = caseFieldId;
    }

    public String getCaseFieldPath() {
        return caseFieldPath;
    }

    public List<String> getCaseFieldPathElements() {
        return Arrays.stream(this.caseFieldPath.trim().split("\\.")).collect(Collectors.toList());
    }

    public void setCaseFieldPath(String caseFieldPath) {
        this.caseFieldPath = caseFieldPath;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String buildCaseFieldId() {
        if (StringUtils.isNotBlank(getCaseFieldPath())) {
            return getCaseFieldId() + '.' + getCaseFieldPath();
        }
        return getCaseFieldId();
    }

    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }
}
