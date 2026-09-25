package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    void shouldCompareEquallyInBothDirectionsAgainstPlainAccessControlList() {
        ComplexACL complexAcl = new ComplexACL("caseworker", true, true, false, true, "Person.Address");
        AccessControlList plainAcl = new AccessControlList("caseworker", true, true, false, true);

        // equals is required to be symmetric. The two types carry different information - a ComplexACL is scoped to
        // one nested element - so neither direction may report equality.
        assertAll(
            () -> assertNotEquals(complexAcl, plainAcl),
            () -> assertNotEquals(plainAcl, complexAcl),
            () -> assertNotEquals(plainAcl.hashCode(), complexAcl.hashCode())
        );
    }

    @Test
    void shouldNotBeRemovableFromAnAclListByAPlainAccessControlList() {
        // CaseFieldDefinition.applyComplexACLs adds ComplexACLs into a nested field's accessControlLists, and both
        // applyComplexACLs and removeACLS then call List.remove(Object) on that list. ArrayList.remove compares with
        // argument.equals(element), so an asymmetric equals lets a plain ACL delete a ComplexACL entry.
        ComplexACL complexAcl = new ComplexACL("caseworker", true, true, false, true, "Person.Address");
        AccessControlList plainAcl = new AccessControlList("caseworker", true, true, false, true);
        List<AccessControlList> accessControlLists = new ArrayList<>(List.of(complexAcl));

        assertFalse(accessControlLists.remove(plainAcl), "a plain ACL must not match a ComplexACL entry");
        assertEquals(List.of(complexAcl), accessControlLists);
    }
}
