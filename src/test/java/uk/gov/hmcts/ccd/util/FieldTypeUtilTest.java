package uk.gov.hmcts.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.search.DataType;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldTypeUtilTest {

    @Test
    void shouldReturnNullWhenFieldIsNull() {
        String result = FieldTypeUtil.getDataTypeFromField(null);
        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenFieldTypeIsNull() {
        Field field = mock(Field.class);
        when(field.getType()).thenReturn(null);

        String result = FieldTypeUtil.getDataTypeFromField(field);
        assertNull(result);
    }

    @Test
    void shouldReturnCollectionWhenFieldTypeIsCollection() {
        Field field = mock(Field.class);
        FieldTypeDefinition fieldType = mock(FieldTypeDefinition.class);
        FieldTypeDefinition collectionFieldType = mock(FieldTypeDefinition.class);

        when(field.getType()).thenReturn(fieldType);
        when(fieldType.getCollectionFieldTypeDefinition()).thenReturn(collectionFieldType);
        when(fieldType.isCollectionFieldType()).thenReturn(true);

        String result = FieldTypeUtil.getDataTypeFromField(field);
        assertEquals(DataType.COLLECTION.toString(), result);
    }

    @Test
    void shouldReturnComplexWhenFieldTypeIsComplex() {
        Field field = mock(Field.class);
        FieldTypeDefinition fieldType = mock(FieldTypeDefinition.class);

        when(field.getType()).thenReturn(fieldType);
        when(fieldType.getCollectionFieldTypeDefinition()).thenReturn(null);
        when(fieldType.isComplexFieldType()).thenReturn(true);
        when(fieldType.getComplexFields()).thenReturn(java.util.Collections.emptyList());

        String result = FieldTypeUtil.getDataTypeFromField(field);
        assertEquals(DataType.COMPLEX.toString(), result);
    }

    @Test
    void shouldReturnNullWhenFieldTypeIsNeitherCollectionNorComplex() {
        Field field = mock(Field.class);
        FieldTypeDefinition fieldType = mock(FieldTypeDefinition.class);

        when(field.getType()).thenReturn(fieldType);
        when(fieldType.getCollectionFieldTypeDefinition()).thenReturn(null);
        when(fieldType.isComplexFieldType()).thenReturn(false);

        String result = FieldTypeUtil.getDataTypeFromField(field);
        assertNull(result);
    }
}
