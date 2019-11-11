package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ToString
public class CaseViewField implements CommonField {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    public static final String READONLY = "READONLY";
    public static final String MANDATORY = "MANDATORY";
    public static final String OPTIONAL = "OPTIONAL";

    private String id;
    private String label;
    @JsonProperty("hint_text")
    private String hintText;
    @JsonProperty("field_type")
    private FieldType fieldType;
    private Boolean hidden;
    @JsonProperty("validation_expr")
    private String validationExpression;
    @JsonProperty("security_label")
    private String securityLabel;
    @JsonProperty("order")
    private Integer order;
    private Object value;
    @JsonProperty("display_context")
    private String displayContext;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;
    @JsonProperty("show_condition")
    private String showCondition;
    @JsonProperty("show_summary_change_option")
    private Boolean showSummaryChangeOption;
    @JsonProperty("show_summary_content_option")
    private Integer showSummaryContentOption;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    private boolean metadata;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Boolean isHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getValidationExpression() {
        return validationExpression;
    }

    public void setValidationExpression(String validationExpression) {
        this.validationExpression = validationExpression;
    }

    public String getSecurityLabel() {
        return securityLabel;
    }

    public void setSecurityLabel(String securityLabel) {
        this.securityLabel = securityLabel;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    public String getShowCondition() {
        return this.showCondition;
    }

    public Boolean getShowSummaryChangeOption() {
        return showSummaryChangeOption;
    }

    public void setShowSummaryChangeOption(final Boolean showSummaryChangeOption) {
        this.showSummaryChangeOption = showSummaryChangeOption;
    }

    public Integer getShowSummaryContentOption() {
        return showSummaryContentOption;
    }

    public void setShowSummaryContentOption(Integer showSummaryContentOption) {
        this.showSummaryContentOption = showSummaryContentOption;
    }

    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

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

    public static CaseViewField createFrom(CaseTypeTabField field, Map<String, ?> data) {
        CaseViewField caseViewField = createFrom(field.getCaseField(), data);
        caseViewField.setOrder(field.getDisplayOrder());
        caseViewField.setShowCondition(field.getShowCondition());
        caseViewField.setDisplayContextParameter(field.getDisplayContextParameter());
        return caseViewField;
    }

    public static CaseViewField createFrom(CaseField caseField, Map<String, ?> data) {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(caseField.getId());
        caseViewField.setLabel(caseField.getLabel());
        caseViewField.setFieldType(caseField.getFieldType());
        caseViewField.setHidden(caseField.getHidden());
        caseViewField.setHintText(caseField.getHintText());
        caseViewField.setSecurityLabel(caseField.getSecurityLabel());
        caseViewField.setValidationExpression(caseField.getFieldType().getRegularExpression());
        caseViewField.setAccessControlLists(caseField.getAccessControlLists());
        sortDataValues(caseField, MAPPER.convertValue(data.get(caseField.getId()), JsonNode.class));
        caseViewField.setValue(data.get(caseField.getId()));
        caseViewField.setMetadata(caseField.isMetadata());

        return caseViewField;
    }

    private static void sortDataValues(final CommonField caseField, JsonNode data) {
        if (data != null) {
            if (caseField.isCompound()) {
                List<CaseField> children = caseField.getFieldType().getChildren();
                children.forEach(childField -> {
                    if (childField.isCollectionFieldType()) {
                        for (int index = 0; index < data.size(); index++) {
                            sortDataValues(childField, data.get(index).get("value").get(childField.getId()));
                        }
                    } else if (childField.isComplexFieldType()) {
                        sortDataValues(childField, data.get(childField.getId()));
                    }
                });
                setOrderForComplexTypeFields(caseField, data, children);
            }
        }
    }

    private static void setOrderForComplexTypeFields(final CommonField caseField, final JsonNode data, final List<CaseField> children) {
        if (caseField.isCollectionFieldType()) {
            for (int index = 0; index < data.size(); index++) {
                JsonNode value = data.get(index).get("value");
                LinkedHashMap valueMap = Maps.newLinkedHashMap();
                for (CaseField field : children) {
                    valueMap.put(field.getId(), value.get(field.getId()));
                }
                ((ObjectNode) data.get(index)).set("value", MAPPER.convertValue(valueMap, JsonNode.class));
            }
        } else if (caseField.isComplexFieldType()) {
            for (CaseField field : children) {
                ((ObjectNode) data).set(field.getId(), data.get(field.getId()));
            }
        }
    }
}
