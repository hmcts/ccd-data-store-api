package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.Serializable;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
public class SearchAliasField implements Serializable {

    private static final long serialVersionUID = -9131437463329052815L;

    String id;
    String caseTypeId;
    String caseFieldPath;
    FieldTypeDefinition fieldTypeDefinition;

    public String getId() {
        return id;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getCaseFieldPath() {
        return caseFieldPath;
    }

    public FieldTypeDefinition getFieldTypeDefinition() {
        return fieldTypeDefinition;
    }
}
