package uk.gov.hmcts.ccd.domain.model.definition;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccessControlListTest {

    @Test
    void shouldValidateAccessControlListParsing() throws JacksonException {
        AccessControlList accessControlList = new AccessControlList();

        accessControlList.setUpdate(false);
        accessControlList.setRead(false);
        accessControlList.setDelete(false);
        accessControlList.setCreate(true);
        accessControlList.setAccessProfile("test");

        ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults().build();
        String value = objectMapper.writeValueAsString(accessControlList);

        assertEquals("{\"create\":true,\"read\":false,\"update\":false,\"delete\":false,\"role\":\"test\"}", value);
    }

    @Test
    void shouldValidateAccessControlListSerializationWithRole() throws JacksonException {
        String accessControlJson = "{\n"
            + "\t\"role\": \"caseworker-probate-public\",\n"
            + "\t\"create\": true,\n"
            + "\t\"read\": true,\n"
            + "\t\"update\": true,\n"
            + "\t\"delete\": false\n"
            + "}";

        ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults().build();
        AccessControlList value = objectMapper.readValue(accessControlJson, AccessControlList.class);
        assertEquals("caseworker-probate-public", value.getAccessProfile());
    }

    @Test
    void shouldValidateAccessControlListSerializationWithAccessProfile() throws JacksonException {
        String accessControlJson = "{\n"
            + "\t\"accessProfile\": \"caseworker-probate-public\",\n"
            + "\t\"create\": true,\n"
            + "\t\"read\": true,\n"
            + "\t\"update\": true,\n"
            + "\t\"delete\": false\n"
            + "}";

        ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults().build();
        AccessControlList value = objectMapper.readValue(accessControlJson, AccessControlList.class);
        assertEquals("caseworker-probate-public", value.getAccessProfile());
    }

    @Test
    void shouldCreateDuplicate() {
        AccessControlList accessControlList = new AccessControlList();

        accessControlList.setUpdate(false);
        accessControlList.setRead(false);
        accessControlList.setDelete(false);
        accessControlList.setCreate(true);
        accessControlList.setAccessProfile("test");

        AccessControlList duplicate  = accessControlList.createCopy();

        assertNotNull(duplicate);
        assertEquals(duplicate.getAccessProfile(), accessControlList.getAccessProfile());
    }

    @Test
    void shouldValidateToString() {
        AccessControlList accessControlList = new AccessControlList();

        accessControlList.setUpdate(false);
        accessControlList.setRead(false);
        accessControlList.setDelete(false);
        accessControlList.setCreate(true);
        accessControlList.setAccessProfile("test");

        assertNotNull("ACL{accessProfile='test', crud=C}", accessControlList.toString());
    }


    @Test
    void shouldValidateToStringWithCRUD() {
        AccessControlList accessControlList = new AccessControlList();

        accessControlList.setUpdate(true);
        accessControlList.setRead(true);
        accessControlList.setDelete(true);
        accessControlList.setCreate(true);
        accessControlList.setAccessProfile("test");

        assertEquals("ACL{accessProfile='test', crud=CRUD}", accessControlList.toString());
    }
}
