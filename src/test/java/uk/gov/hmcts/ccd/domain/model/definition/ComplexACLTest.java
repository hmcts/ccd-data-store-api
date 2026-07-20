package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexACLTest {

    @Test
    void shouldExposeConstructorValues() {
        ComplexACL acl = new ComplexACL("caseworker", true, true, false, true, "Person.Address");

        assertAll(
            () -> assertEquals("Person.Address", acl.getListElementCode()),
            () -> assertEquals("caseworker", acl.getAccessProfile()),
            () -> assertTrue(acl.isCreate()),
            () -> assertTrue(acl.isRead()),
            () -> assertFalse(acl.isUpdate()),
            () -> assertTrue(acl.isDelete()),
            () -> assertEquals("ACL{accessProfile='caseworker', crud=CRD}, listElementCode='Person.Address'",
                acl.toString())
        );
    }

    @Test
    void shouldCompareAccessProfileAndListElementCode() {
        ComplexACL acl = new ComplexACL("caseworker", true, true, false, true, "Person.Address");
        ComplexACL sameAcl = new ComplexACL("caseworker", true, true, false, true, "Person.Address");
        ComplexACL differentListElementCode = new ComplexACL("caseworker", true, true, false, true, "Person.Name");
        ComplexACL differentAccessProfile = new ComplexACL("citizen", true, true, false, true, "Person.Address");
        AccessControlList baseAcl = new AccessControlList("caseworker", true, true, false, true);

        assertAll(
            () -> assertEquals(acl, sameAcl),
            () -> assertEquals(acl.hashCode(), sameAcl.hashCode()),
            () -> assertNotEquals(acl, differentListElementCode),
            () -> assertNotEquals(acl, differentAccessProfile),
            () -> assertNotEquals(acl, baseAcl)
        );
    }
}
