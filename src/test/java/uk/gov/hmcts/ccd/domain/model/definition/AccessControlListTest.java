package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccessControlListTest {

    @Test
    void shouldValidateAccessControlListParsing() throws JsonProcessingException {
        AccessControlList accessControlList = AccessControlList.builder()
            .accessProfile("test")
            .create(true)
            .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String value = objectMapper.writeValueAsString(accessControlList);

        assertEquals("{\"create\":true,\"read\":false,\"update\":false,\"delete\":false,\"role\":\"test\"}", value);
    }

    @Test
    void shouldValidateAccessControlListSerializationWithRole() throws JsonProcessingException {
        String accessControlJson = """
            {
                "role": "caseworker-probate-public",
                "create": true,
                "read": true,
                "update": true,
                "delete": false
            }""";

        ObjectMapper objectMapper = new ObjectMapper();
        AccessControlList value = objectMapper.readValue(accessControlJson, AccessControlList.class);
        assertEquals("caseworker-probate-public", value.getAccessProfile());
    }

    @Test
    void shouldValidateAccessControlListSerializationWithAccessProfile() throws JsonProcessingException {
        String accessControlJson = """
            {
                "accessProfile": "caseworker-probate-public",
                "create": true,
                "read": true,
                "update": true,
                "delete": false
            }""";

        ObjectMapper objectMapper = new ObjectMapper();
        AccessControlList value = objectMapper.readValue(accessControlJson, AccessControlList.class);
        assertEquals("caseworker-probate-public", value.getAccessProfile());
    }

    @Test
    void shouldCreateDuplicate() {
        AccessControlList accessControlList = AccessControlList.builder()
            .accessProfile("test")
            .create(true)
            .build();

        AccessControlList duplicate  = accessControlList.duplicate();

        assertNotNull(duplicate);
        assertEquals(duplicate.getAccessProfile(), accessControlList.getAccessProfile());
    }

    @Test
    void shouldValidateToString() {
        AccessControlList accessControlList = AccessControlList.builder()
            .accessProfile("test")
            .create(true)
            .build();

        assertEquals("ACL{accessProfile='test', crud=C}", accessControlList.toString());
    }


    @Test
    void shouldValidateToStringWithCRUD() {
        AccessControlList accessControlList = AccessControlList.builder()
            .accessProfile("test")
            .create(true)
            .read(true)
            .update(true)
            .delete(true)
            .build();

        assertEquals("ACL{accessProfile='test', crud=CRUD}", accessControlList.toString());
    }
}
