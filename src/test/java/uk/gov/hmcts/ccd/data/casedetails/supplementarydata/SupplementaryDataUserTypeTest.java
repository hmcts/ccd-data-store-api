package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.hibernate.type.CustomType;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class SupplementaryDataUserTypeTest {

    @Test
    public void checkSupplementaryDataUserType_fields() throws Exception {
        SupplementaryDataUserType supplementaryDataUserType = new SupplementaryDataUserType();
        
        assertEquals(SqlTypes.JSON, supplementaryDataUserType.getSqlType());
        assertEquals(JsonNode.class, supplementaryDataUserType.returnedClass());
        assertEquals(true, supplementaryDataUserType.isMutable());

        JsonNode jsonNode = JsonNodeFactory.instance.objectNode();
        assertEquals(true, supplementaryDataUserType.equals(jsonNode, jsonNode));
        assertEquals(jsonNode.hashCode(), supplementaryDataUserType.hashCode(jsonNode));
        assertThrows(AssertionError.class, () -> supplementaryDataUserType.hashCode(null));
        assertEquals(jsonNode, supplementaryDataUserType.deepCopy(jsonNode));
        assertNull(supplementaryDataUserType.deepCopy(null));
        assertEquals(jsonNode, supplementaryDataUserType.replace(jsonNode, null, null));
        assertEquals(jsonNode, supplementaryDataUserType.assemble((Serializable)jsonNode, null));
        assertNull(supplementaryDataUserType.assemble(null, null));
        assertEquals((Serializable)jsonNode, supplementaryDataUserType.disassemble(jsonNode));
        assertNull(supplementaryDataUserType.disassemble(null));

        PreparedStatement psMock = Mockito.mock(PreparedStatement.class);
        supplementaryDataUserType.nullSafeSet(psMock, null, 0, null);
        verify(psMock).setNull(anyInt(), anyInt());

        supplementaryDataUserType.nullSafeSet(psMock, jsonNode, 0, null);
        verify(psMock).setObject(anyInt(), anyString(), anyInt());

        doThrow(RuntimeException.class).when(psMock).setObject(anyInt(), anyString(), anyInt());
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> 
            supplementaryDataUserType.nullSafeSet(psMock, jsonNode, 0, null));
        assertEquals("Failed to convert MyJson to String: null", ex2.getMessage());

        ResultSet rsMock = Mockito.mock(ResultSet.class);
        when(rsMock.getString(0)).thenThrow(new RuntimeException());
        RuntimeException ex3 = assertThrows(RuntimeException.class, () -> 
            supplementaryDataUserType.nullSafeGet(rsMock, 0, null, null));
        assertEquals("Failed to convert String to MyJson: null", ex3.getMessage());
    }

    @Test
    public void testCustomType() {
        CustomType<JsonNode> customClass = SupplementaryDataUserType.CUSTOM_TYPE;
        assertNotNull(customClass);
        assertEquals(CustomType.class, customClass.getClass());
        assertEquals(JsonNode.class, customClass.getJavaType());
    }
    
}
