package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ComplexACLTest {

    @Test
    void shouldCreateDeepCopy() {
        ComplexACL acl = new ComplexACL();
        acl.setListElementCode("Person.Address");
        acl.setAccessProfile("caseworker");
        acl.setCreate(true);
        acl.setRead(true);
        acl.setUpdate(false);
        acl.setDelete(true);

        ComplexACL copy = acl.deepCopy();

        assertAll(
            () -> assertNotSame(acl, copy),
            () -> assertEquals(acl.getListElementCode(), copy.getListElementCode()),
            () -> assertEquals(acl.getAccessProfile(), copy.getAccessProfile()),
            () -> assertEquals(acl.isCreate(), copy.isCreate()),
            () -> assertEquals(acl.isRead(), copy.isRead()),
            () -> assertEquals(acl.isUpdate(), copy.isUpdate()),
            () -> assertEquals(acl.isDelete(), copy.isDelete())
        );
    }
}
