package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.type.CustomType;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class SupplementaryDataUserTypeTest {

    @Test
    public void checkSupplementaryDataUserType_fields() throws SQLException {
        SupplementaryDataUserType supplementaryDataUserType = new SupplementaryDataUserType();
        
        assertEquals(SqlTypes.JSON, supplementaryDataUserType.getSqlType());
        assertEquals(JsonNode.class, supplementaryDataUserType.returnedClass());
        assertEquals(true, supplementaryDataUserType.isMutable());

        JsonNode jsonNode = JsonNodeFactory.instance.objectNode();
        assertEquals(true, supplementaryDataUserType.equals(jsonNode, jsonNode));
        assertEquals(jsonNode.hashCode(), supplementaryDataUserType.hashCode(jsonNode));
        assertEquals(jsonNode, supplementaryDataUserType.deepCopy(jsonNode));
        assertEquals(jsonNode, supplementaryDataUserType.replace(jsonNode, null, null));
        assertEquals(jsonNode, supplementaryDataUserType.assemble((Serializable)jsonNode, null));
        assertEquals((Serializable)jsonNode, supplementaryDataUserType.disassemble(jsonNode));

        PreparedStatement psMock = Mockito.mock(PreparedStatement.class);
        supplementaryDataUserType.nullSafeSet(psMock, null, 0, null);
        verify(psMock).setNull(anyInt(), anyInt());

        supplementaryDataUserType.nullSafeSet(psMock, jsonNode, 0, null);
        verify(psMock).setObject(anyInt(), anyString(), anyInt());
    }

    @Test
    public void testCustomType() {
        CustomType<JsonNode> customClass = SupplementaryDataUserType.CUSTOM_TYPE;
        assertNotNull(customClass);
        assertEquals(CustomType.class, customClass.getClass());
        assertEquals(JsonNode.class, customClass.getJavaType());
    }
    
}
