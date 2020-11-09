package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.util.Map;

public class ValidationContext {

    private final CaseDataContent currentCaseDataContent;
    private final String caseTypeId;
    private final CaseTypeDefinition caseTypeDefinition;
    private final Map<String, JsonNode> data;
    private String fieldId;
    private String path;
    private JsonNode dataValue;
    private CaseFieldDefinition caseFieldDefinition;

    public ValidationContext() {
        this(null, null, null, null);
    }

    public ValidationContext(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition) {
        this(null, null, caseTypeDefinition, data);
    }

    public ValidationContext(
                            CaseDataContent currentCaseDataContent,
                            String caseTypeId,
                            CaseTypeDefinition caseTypeDefinition,
                            Map<String, JsonNode> data
    ) {
        this.currentCaseDataContent = currentCaseDataContent;
        this.caseTypeId = caseTypeId;
        this.caseTypeDefinition = caseTypeDefinition;
        this.data = data;
    }


    public CaseDataContent getCurrentCaseDataContent() {
        return currentCaseDataContent;
    }

    public String getCaseTypeId() {
        return caseTypeId;
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

    public JsonNode getDataValue() {
        return dataValue;
    }

    public void setDataValue(JsonNode dataValue) {
        this.dataValue = dataValue;
    }

    public CaseFieldDefinition getCaseFieldDefinition() {
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
