package uk.gov.hmcts.ccd.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.Field;

import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

@UtilityClass
public class FieldTestUtil {

    public static final String COMPLEX_FIELD = "ComplexField";
    public static final String COLLECTION_FIELD = "CollectionField";
    public static final String TEXT_TYPE = "Text";

    public static Field field(String id, FieldTypeDefinition fieldType) {
        Field field = new Field();
        field.setId(id);
        field.setType(fieldType);
        return field;
    }

    public static FieldTypeDefinition fieldType(String id, String type, List<CaseFieldDefinition> complexFields,
                                          FieldTypeDefinition collectionFieldType) {
        FieldTypeDefinition fieldType = new FieldTypeDefinition();
        fieldType.setId(id);
        fieldType.setType(type);
        fieldType.setComplexFields(complexFields);
        fieldType.setCollectionFieldTypeDefinition(collectionFieldType);
        return fieldType;
    }

    public static FieldTypeDefinition fieldType(String fieldType) {
        return fieldType(fieldType, fieldType, Collections.emptyList(), null);
    }

    public static CaseFieldDefinition simpleField(final String id, final Integer order) {
        return newCaseField()
            .withId(id)
            .withFieldType(simpleType())
            .withOrder(order)
            .build();
    }

    public static FieldTypeDefinition simpleType() {
        return aFieldType().withType(TEXT_TYPE).build();
    }
}
