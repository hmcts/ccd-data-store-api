package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

@ToString
@ApiModel(description = "")
public class CaseField implements Serializable {

    private static final long serialVersionUID = -4257574164546267919L;

    private String id = null;
    @JsonProperty("case_type_id")
    private String caseTypeId = null;
    private String label = null;
    @JsonProperty("hint_text")
    private String hintText = null;
    @JsonProperty("field_type")
    private FieldType fieldType = null;
    private Boolean hidden = null;
    @JsonProperty("security_classification")
    private String securityLabel = null;
    @JsonProperty("live_from")
    private String liveFrom = null;
    @JsonProperty("live_until")
    private String liveUntil = null;
    @JsonProperty("show_condition")
    private String showConditon = null;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    private boolean metadata;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getSecurityLabel() {
        return securityLabel;
    }

    public void setSecurityLabel(String securityLabel) {
        this.securityLabel = securityLabel;
    }

    public String getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(String liveFrom) {
        this.liveFrom = liveFrom;
    }

    public String getLiveUntil() {
        return liveUntil;
    }

    public void setLiveUntil(String liveUntil) {
        this.liveUntil = liveUntil;
    }

    public String getShowConditon() { return showConditon; }

    public void setShowConditon(String showConditon) { this.showConditon = showConditon; }

    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }

    public void setAccessControlLists(List<AccessControlList> accessControlLists) {
        this.accessControlLists = accessControlLists;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    @JsonIgnore
    public boolean isCollectionFieldType() {
        return FieldType.COLLECTION.equalsIgnoreCase(fieldType.getType());
    }

    @JsonIgnore
    public void propagateACLsToNestedFields() {
        propagateACLsToNestedFields(this, this.accessControlLists);
    }

    @JsonIgnore
    private static void propagateACLsToNestedFields(CaseField caseField, List<AccessControlList> acls) {
        if (caseField.getFieldType().getType().equalsIgnoreCase(COMPLEX) || caseField.getFieldType().getType().equalsIgnoreCase(COLLECTION)) {
            caseField.getFieldType().getChildren().forEach(nestedField -> {
                if (nestedField.getAccessControlLists() == null || nestedField.getAccessControlLists().isEmpty()) {
                    nestedField.setAccessControlLists(acls);
                }
                propagateACLsToNestedFields(nestedField, acls);
            });
        }
    }

    @JsonIgnore
    public CaseField findNestedElementByPath(String path) {
        if (StringUtils.isBlank(path)) {
            throw new RuntimeException(format("Invalid blank element path for field %s.", this.id));
        }
        if (this.getFieldType().getChildren().isEmpty()) {
            throw new RuntimeException(format("CaseField %s has no nested elements.", this.id));
        }
        List<String> pathElements = Arrays.stream(path.trim().split("\\.")).collect(Collectors.toList());

        return reduce(this.getFieldType().getChildren(), pathElements);
    }

    @JsonIgnore
    private CaseField reduce(List<CaseField> caseFields, List<String> pathElements) {
        String head = pathElements.get(0);
        if (pathElements.size() == 1) {
            return caseFields.stream().filter(e -> e.getId().equals(head)).findFirst()
                .orElseThrow(() -> new RuntimeException(format("Nested element not found for %s", head)));
        } else {
            CaseField caseField = caseFields.stream().filter(e -> e.getId().equals(head)).findFirst()
                .orElseThrow(() -> new RuntimeException(format("Nested element not found for %s", head)));

            List<CaseField> newCaseFields = caseField.getFieldType().getChildren();
            List<String> tail = pathElements.subList(1, pathElements.size());

            return reduce(newCaseFields, tail);
        }
    }
}
