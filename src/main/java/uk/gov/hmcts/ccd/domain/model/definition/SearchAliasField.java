package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class SearchAliasField implements Serializable, Copyable<SearchAliasField> {

    private static final long serialVersionUID = -9131437463329052815L;

    private String id;
    private String caseTypeId;
    private String caseFieldPath;
    private FieldTypeDefinition fieldTypeDefinition;

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

    public FieldTypeDefinition getFieldTypeDefinition() {
        return fieldTypeDefinition;
    }

    public void setFieldTypeDefinition(FieldTypeDefinition fieldTypeDefinition) {
        this.fieldTypeDefinition = fieldTypeDefinition;
    }

    @JsonIgnore
    @Override
    public SearchAliasField createCopy() {
        SearchAliasField copy = new SearchAliasField();
        copy.setId(this.id);
        copy.setCaseTypeId(this.caseTypeId);
        copy.setCaseFieldPath(this.caseFieldPath);
        copy.setFieldTypeDefinition(this.fieldTypeDefinition != null ? this.fieldTypeDefinition.createCopy() : null);
        return copy;
    }
}
