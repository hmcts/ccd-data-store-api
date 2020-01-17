package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

public class SearchAliasField implements Serializable {

    private static final long serialVersionUID = -9131437463329052815L;
    
    private String id;
    private String caseTypeId;
    private String caseFieldPath;
    private FieldType fieldType;

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

    public String getCaseFieldPath() {
        return caseFieldPath;
    }

    public void setCaseFieldPath(String caseFieldPath) {
        this.caseFieldPath = caseFieldPath;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }
}
