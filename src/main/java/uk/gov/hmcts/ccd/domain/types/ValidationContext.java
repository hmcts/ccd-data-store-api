package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.Map;

public class ValidationContext {

    private final CaseTypeDefinition caseTypeDefinition;
    private final Map<String, JsonNode> data;
    private String fieldId;
    private String path;
    private JsonNode fieldValue;
    private CaseFieldDefinition caseFieldDefinition;

    public ValidationContext(
        CaseTypeDefinition caseTypeDefinition,
        Map<String, JsonNode> data
    ) {
        this.caseTypeDefinition = caseTypeDefinition;
        this.data = data;
    }

    public String getCaseTypeId() {
        return caseTypeDefinition.getId();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public CaseTypeDefinition getCaseTypeDefinition() {
        return caseTypeDefinition;
    }

    public Map<String, JsonNode> getData() {
        return data;
    }

    public JsonNode getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(JsonNode fieldValue) {
        this.fieldValue = fieldValue;
    }

    public CaseFieldDefinition getFieldDefinition() {
        return caseFieldDefinition;
    }

    public void setCaseFieldDefinition(CaseFieldDefinition caseFieldDefinition) {
        this.caseFieldDefinition = caseFieldDefinition;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }
}
